package cn.xej.api.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.*;
import java.util.Map;

public abstract class AbstractClient {
    protected final String endpoint;
    protected final Credential credential;

    protected final OkHttpClient okHttpClient;
    protected final ObjectMapper objectMapper;

    public AbstractClient(String endpoint, Credential credential) {
        this.okHttpClient = okHttpClient();
        this.objectMapper = new ObjectMapper();
        this.endpoint = endpoint + "/";
        this.credential = credential;
    }

    private OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // 连接超时
                .readTimeout(20, TimeUnit.SECONDS)   // 读数据超时
                .writeTimeout(20, TimeUnit.SECONDS)  // 写数据超时
                .retryOnConnectionFailure(true)      // 失败重连
                .build();
    }

    protected  <T> T internalRequest(AbstractModel request, String actionName, Class<T> typeOfT) throws ApiSDKException {
        return executeWithRetry(actionName, () -> {
            Request httpRequest = null;
            try {

                String url = "http://" + endpoint + actionName;
                //1. 序列化Body，因为签名需要用到Body的内容
                String jsonBody = writeValue(request);
                RequestBody body = RequestBody.create(
                        jsonBody,
                        MediaType.get("application/json; charset=utf-8")
                );

                // 2. 准备 Request 构建器
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

                // 3. 注入认证Header
                enrichRequestWithAuth(requestBuilder, actionName, jsonBody);

                httpRequest = requestBuilder.build();
                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    // 从响应头中获取requestId
                    String requestId = response.header("X-TC-RequestId");
                    
                    int code = response.code();
                    if (is4xx(code) || is5xx(code)) {
                        // 业务项目报错code
                        Map<String, Object> errorResponse = readValue(response.body().string(), Map.class);
                        String errorCode = (String) errorResponse.get("code"); 
                        String errorMsg = (String) errorResponse.get("message"); 
                        throw new ApiSDKException(errorMsg, requestId, errorCode);
                    }

                    String responseBody = response.body().string();
                    
                    // 反序列化响应体
                    T result = readValue(responseBody, typeOfT);
                    
                    // 如果结果对象有setRequestId方法，注入requestId
                    if (result != null) {
                        try {
                            java.lang.reflect.Method setRequestIdMethod = result.getClass().getMethod("setRequestId", String.class);
                            setRequestIdMethod.invoke(result, requestId);
                        } catch (Exception e) {
                            // 如果没有setRequestId方法，忽略
                            System.out.println("Warning: Result object does not have setRequestId method");
                        }
                    }
                    
                    return result;
                }
            } catch (ApiSDKException e) {
                // 如果已经是ApiSDKException，直接重新抛出，保留原始的errorCode和requestId
                throw e;
            } catch (IOException e) {
                // 网络IO异常，包装成 RuntimeException 供 RetryConfig 识别
                throw new RuntimeException(e); 
            } catch (Exception e) {
                // 注意：这里无法直接获取response，所以requestId可能为null
                // 在实际项目中，可以考虑在请求构建时生成requestId
                throw new ApiSDKException(e.getMessage(), "", "");
            }
        });
    }

    private void enrichRequestWithAuth(Request.Builder builder, String action, String payload) {
        // 1. 获取当前时间戳
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // 2. 设置腾讯云风格的公共 Header
        builder.addHeader("X-TC-Action", action); // 接口名
        builder.addHeader("X-TC-Version", "2025-12-24"); // 版本号
        builder.addHeader("X-TC-Timestamp", timestamp);
        
        // 3. 计算签名 (简化版：Signature = HMAC-SHA256(SecretKey, StringToSign))
        // StringToSign 包含：时间戳 + 动作 + Body内容
        String stringToSign = "POST" + action + timestamp + payload;
        
        try {
            String signature = hmac256(credential.getSecretKey(), stringToSign);
            // 4. 构造 Authorization 头
            // 格式参考：TC3-HMAC-SHA256 Credential=ID/..., SignedHeaders=..., Signature=...
            // 这里简化为直接放 Token 或标准 Auth 头
            String authHeader = String.format("TC3-HMAC-SHA256 Credential=%s, Signature=%s", 
                                            credential.getSecretId(), signature);
            
            builder.addHeader("Authorization", authHeader);
            
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
        // 通常转为 Hex 字符串，这里用 Base64 也可以，看服务端要求
        return bytesToHex(hash); 
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
        int maxAttempts = 2; // 默认不重试
        if (actionName.startsWith("Describe") || actionName.startsWith("Inquiry")) {
            maxAttempts = 3; // Describe和Inquiry开头的action重试3次
        }
        
        // 创建重试配置
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(java.time.Duration.ofMillis(100))
                .retryOnException(throwable -> {
                    // 1. 如果直接是 ApiSDKException，说明是业务报错（4xx/5xx），绝对不重试
                    if (throwable instanceof ApiSDKException) {
                        return false;
                    }
                    
                    // 2. 如果是 RuntimeException 包装的 IOException，重试
                    if (throwable instanceof RuntimeException && throwable.getCause() instanceof IOException) {
                        return true;
                    }

                    // 3. 如果原本就是 IOException，重试
                    return throwable instanceof IOException;
                })
                .build();
        
        Retry retry = Retry.of(actionName, config);
        
        try {
            return Retry.decorateSupplier(retry, action).get();
        } catch (RuntimeException e) {
            // 检查异常链中是否包含ApiSDKException
            Throwable current = e;
            while (current != null) {
                if (current instanceof ApiSDKException) {
                    throw (ApiSDKException) current;
                }
                current = current.getCause();
            }
             // 处理网络异常（还原 IO 异常）
            if (e.getCause() instanceof IOException) {
                throw new ApiSDKException("Network error", "", "NETWORK_ERROR", e);
            }

            // 其他未知错误
            throw new ApiSDKException("Request failed after retries", "", "INTERNAL_ERROR", e);
        }
    }

    private boolean is4xx(Number code) {
        return code.intValue() >= 400 && code.intValue() < 500;
    }

    private boolean is5xx(Number code) {
        return code.intValue() >= 500 && code.intValue() < 600;
    }

    private String writeValue(Object obj) throws IOException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IOException("Serialize error", e);
        }
    }

    private <T> T readValue(String json, Class<T> clazz) throws IOException {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IOException("Parse error", e);
        }
    }

}
