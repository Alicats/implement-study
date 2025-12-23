package cn.xej.api.common;

import java.io.IOException;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.*;

public abstract class AbstractClient {
    protected final String baseUrl;

    protected final OkHttpClient okHttpClient;
    protected final ObjectMapper objectMapper;
    protected final Retry retry;
    protected int count = 1;

    public AbstractClient(String baseUrl) {
       // 修改重试配置，确保异常能被正确识别和重试
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(java.time.Duration.ofMillis(100)) // 简单的固定等待时间
                .retryOnException(throwable -> {
                    // 添加调试日志
                    System.out.println("检查异常是否可重试: " + throwable.getClass().getName());
                    if (throwable instanceof RuntimeException) {
                        return throwable.getCause() instanceof IOException && !(throwable.getCause() instanceof ApiSDKException);
                    }
                    return throwable instanceof IOException;
                })
                .build();
        
        this.retry = Retry.of("myApi", config);
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    protected  <T> T internalRequest(AbstractModel request, String actionName, Class<T> typeOfT) throws ApiSDKException {
        return executeWithRetry(() -> {
            try {

                String url = baseUrl + actionName;
                RequestBody body = RequestBody.create(
                        writeValue(request),
                        MediaType.get("application/json; charset=utf-8")
                );
                Request httpRequest = new Request.Builder().url(url).post(body).build();

                System.out.println("调用api接口次数: " + count);
                count++;
                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    validateResponse(response);
                    String responseBody = response.body().string();
                    return readValue(responseBody, typeOfT);
                }
            } catch (Exception e) {
                throw new ApiSDKException("", e);
            }
        });
    }



    // 🔁 核心：统一执行 + 重试
    private <T> T executeWithRetry(Supplier<T> action) throws ApiSDKException {
        try {
            return Retry.decorateSupplier(retry, action).get();
        } catch (RuntimeException e) {
            // 将 Resilience4j 包装的异常还原为原始 IOException
            if (e.getCause() instanceof IOException) {
                throw e;
            }
            throw new ApiSDKException("Request failed after retries", e);
        }
    }

    // 🚨 统一响应校验（抛异常才能触发重试！）
    private void validateResponse(Response response) throws ApiSDKException {
        int code = response.code();
        if (code != 200) {
            String msg =  "response code is " + code + ", not 200";
            throw new ApiSDKException(msg, "", "ServerSideError");
        }
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
