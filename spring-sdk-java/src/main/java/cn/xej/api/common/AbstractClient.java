package cn.xej.api.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public abstract class AbstractClient {
    // 日志记录器
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final String endpoint;
    protected final Credential credential;

    protected final OkHttpClient okHttpClient;
    
    // 重试配置默认值
    protected final int defaultMaxAttempts = 1;
    protected final long defaultWaitDuration = 100;
    protected final int describeMaxAttempts = 3;

    public AbstractClient(String endpoint, Credential credential) {
        this.okHttpClient = okHttpClient();
        this.endpoint = endpoint + "/";
        this.credential = credential;
    }
    
    /**
     * 带有自定义重试配置的构造函数
     *
     * @param endpoint      服务端点
     * @param credential    凭证信息
     * @param maxAttempts   最大重试次数
     * @param waitDuration  重试间隔（毫秒）
     */
    public AbstractClient(String endpoint, Credential credential, int maxAttempts, long waitDuration) {
        this(endpoint, credential);
    }

    private OkHttpClient okHttpClient() {
        logger.debug("Creating OkHttpClient with connect timeout: 60s, read timeout: 20s, write timeout: 20s");
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // 连接超时
                .readTimeout(20, TimeUnit.SECONDS)   // 读数据超时
                .writeTimeout(20, TimeUnit.SECONDS)  // 写数据超时
                .retryOnConnectionFailure(true)      // 失败重连
                .build();
    }


    
    /**
     * 执行HTTP请求，支持自定义请求头和HTTP方法
     *
     * @param request     请求对象
     * @param actionName  动作名称
     * @param typeOfT     响应类型
     * @param method      HTTP方法
     * @param <T>         响应泛型
     * @return 响应对象
     * @throws ApiSDKException API SDK异常
     */
    protected <T> T internalRequest(AbstractModel request, String actionName, Class<T> typeOfT) throws ApiSDKException {
        logger.info("Starting request: {} with action: {}", endpoint + actionName, actionName);
        logger.debug("Request parameters: {}", request.toJson());
        
        return executeWithRetry(actionName, () -> {
            Request httpRequest = null;
            try {
                String url = "http://" + endpoint + actionName;
                // 1. 序列化Body，因为签名需要用到Body的内容
                String jsonBody = request.toJson();
                
                // 2. 准备 Request 构建器
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url);
                
                
                 //3. 默认使用POST方法
                RequestBody body = RequestBody.create(
                        jsonBody,
                        MediaType.get("application/json; charset=utf-8")
                );
                requestBuilder.post(body);
                

                // 4. 注入认证Header
                enrichRequestWithAuth(requestBuilder, actionName, jsonBody);

                httpRequest = requestBuilder.build();
                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    // 从响应头中获取requestId
                    String requestId = response.header("X-TC-RequestId");
                    
                    int code = response.code();
                    String responseBody = response.body().string();
                    
                    logger.info("Request completed: {} with status code: {}, RequestId: {}", actionName, code, requestId);
                    
                    if (is4xx(code) || is5xx(code)) {
                        // 服务端报错
                        Map<String, Object> errorResponse = AbstractModel.fromJson(responseBody, Map.class);
                        String errorCode = (String) errorResponse.getOrDefault("code", "");
                        String errorMsg = (String) errorResponse.getOrDefault("message", "Unknown error");
                        logger.error("Request failed: {} with error code: {}, message: {}, response: {}", actionName, errorCode, errorMsg, responseBody);
                        throw new ApiSDKException(errorMsg, requestId, errorCode);
                    }

                    // 反序列化响应体
                    T result = AbstractModel.fromJson(responseBody, typeOfT);
                    
                    // 如果结果对象有setRequestId方法，注入requestId
                    if (result != null) {
                        try {
                            java.lang.reflect.Method setRequestIdMethod = result.getClass().getMethod("setRequestId", String.class);
                            setRequestIdMethod.invoke(result, requestId);
                        } catch (Exception e) {
                            // 如果没有setRequestId方法，忽略
                            logger.debug("Result object does not have setRequestId method");
                        }
                    }
                    
                    return result;
                }
            } catch (ApiSDKException e) {
                // 如果已经是ApiSDKException，直接重新抛出，保留原始的errorCode和requestId
                logger.error("API SDK Exception: {} - {}", e.getErrorCode(), e.getMessage(), e);
                throw e;
            } catch (IOException e) {
                // 网络IO异常，包装成 RuntimeException 供 RetryConfig 识别
                logger.error("Network IO Exception: {}", e.getMessage(), e);
                throw new RuntimeException(e); 
            } catch (Exception e) {
                // 注意：这里无法直接获取response，所以requestId可能为null
                // 在实际项目中，可以考虑在请求构建时生成requestId
                logger.error("Unexpected Exception: {}", e.getMessage(), e);
                throw new ApiSDKException(e.getMessage(), "", "", e);
            }
        });
    }

    private void enrichRequestWithAuth(Request.Builder builder, String action, String payload) {
        // 1. 获取当前时间戳
        long timestamp = System.currentTimeMillis() / 1000;
        String timestampStr = String.valueOf(timestamp);

        // 2. 设置公共 Header
        builder.addHeader("X-TC-Action", action); // 接口名
        builder.addHeader("X-TC-Version", "2025-12-24"); // 版本号
        builder.addHeader("X-TC-Timestamp", timestampStr);
        
        // 3. 计算请求体哈希
        String hashedRequestPayload = sha256Hex(payload);
        
        // 4. 构建签名摘要字符串
        String algorithm = "HMAC-SHA256";
        String httpRequestMethod = "POST";
       
        String stringToSign = String.format("%s\n%s\n%s\n%s", 
                httpRequestMethod, action, timestampStr, hashedRequestPayload);
        
        try {
            // 5. 计算签名
            String signature = hmac256(credential.getSecretKey(), stringToSign);
            
            // 6. 构造 Authorization 头
            String authHeader = String.format("%s Credential=%s, Signature=%s", 
                    algorithm, credential.getSecretId(), signature);
            
            builder.addHeader("Authorization", authHeader);
            builder.addHeader("Content-Type", "application/json; charset=utf-8");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    // HMAC-SHA256 算法工具
    private String hmac256(String key, String msg) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash); 
    }

    /**
     * SHA256 哈希计算
     *
     * @param data 输入数据
     * @return 十六进制格式的哈希值
     */
    private String sha256Hex(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    // 🔁 核心：统一执行 + 重试
    private <T> T executeWithRetry(String actionName, Supplier<T> action) throws ApiSDKException {
        // 根据actionName动态配置重试策略
        int maxAttempts = defaultMaxAttempts;
        if (actionName.startsWith("Describe") || actionName.startsWith("Inquiry")) {
            maxAttempts = describeMaxAttempts;
        }
        
        logger.info("Configuring retry policy for action: {}, max attempts: {}", actionName, maxAttempts);
        
        // 创建重试配置，支持指数退避策略
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(java.time.Duration.ofMillis(defaultWaitDuration))
                .retryOnException(throwable -> {
                    // 1. 如果直接是 ApiSDKException，说明是业务报错（4xx/5xx），绝对不重试
                    if (throwable instanceof ApiSDKException) {
                        logger.debug("Not retrying because exception is ApiSDKException: {}", throwable.getMessage());
                        return false;
                    }
                    
                    // 2. 如果是 RuntimeException 包装的 IOException，重试
                    boolean shouldRetry = (throwable instanceof RuntimeException && throwable.getCause() instanceof IOException) 
                            || throwable instanceof IOException;
                    
                    if (shouldRetry) {
                        logger.debug("Will retry request for action: {}, because of exception: {}", actionName, throwable.getMessage());
                    } else {
                        logger.debug("Not retrying request for action: {}, because of exception: {}", actionName, throwable.getMessage());
                    }
                    
                    return shouldRetry;
                })
                .build();
        
        Retry retry = Retry.of(actionName, config);
        
        try {
            logger.debug("Executing request with retry for action: {}", actionName);
            return Retry.decorateSupplier(retry, action).get();
        } catch (RuntimeException e) {
            // 检查异常链中是否包含ApiSDKException
            Throwable current = e;
            while (current != null) {
                if (current instanceof ApiSDKException) {
                    logger.error("Retry failed for action: {} with ApiSDKException: {} - {}", 
                            actionName, ((ApiSDKException) current).getErrorCode(), current.getMessage());
                    throw (ApiSDKException) current;
                }
                current = current.getCause();
            }
             // 处理网络异常（还原 IO 异常）
            if (e.getCause() instanceof IOException) {
                logger.error("Retry failed for action: {} with network error after {} attempts", 
                        actionName, maxAttempts, e);
                throw new ApiSDKException("Network error", "", "NETWORK_ERROR", e);
            }

            // 其他未知错误
            logger.error("Retry failed for action: {} with unexpected error after {} attempts", 
                    actionName, maxAttempts, e);
            throw new ApiSDKException("Request failed after retries", "", "INTERNAL_ERROR", e);
        }
    }

    private boolean is4xx(Number code) {
        return code.intValue() >= 400 && code.intValue() < 500;
    }

    private boolean is5xx(Number code) {
        return code.intValue() >= 500 && code.intValue() < 600;
    }

}
