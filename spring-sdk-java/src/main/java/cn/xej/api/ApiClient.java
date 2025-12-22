package cn.xej.api;

import cn.xej.api.common.NonRetryableException;
import cn.xej.api.common.RetryableException;
import cn.xej.api.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.*;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * 统一API客户端
 */
public class ApiClient {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Retry retry;
    private int exceptionCount = 0; // 记录异常次数

    public ApiClient(String baseUrl) {
        // 修改重试配置，确保异常能被正确识别和重试
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(java.time.Duration.ofMillis(100)) // 简单的固定等待时间
                .retryOnException(throwable -> {
                    // 添加调试日志
                    System.out.println("检查异常是否可重试: " + throwable.getClass().getName());
                    if (throwable instanceof RuntimeException) {
                        System.out.println("RuntimeException的Cause: " + (throwable.getCause() != null ? throwable.getCause().getClass().getName() : "无"));
                        return throwable.getCause() instanceof IOException && !(throwable.getCause() instanceof NonRetryableException);
                    }
                    return throwable instanceof IOException && !(throwable instanceof NonRetryableException);
                })
                .build();
        
        this.retry = Retry.of("myApi", config);
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    // 🔁 核心：统一执行 + 重试
    private <T> T executeWithRetry(Supplier<T> action) throws IOException {
        try {
            return Retry.decorateSupplier(retry, action).get();
        } catch (RuntimeException e) {
            // 将 Resilience4j 包装的异常还原为原始 IOException
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException("Request failed after retries", e);
        }
    }

    // 🚨 统一响应校验（抛异常才能触发重试！）
    private void validateResponse(Response response) throws IOException {
        int code = response.code();
        if (code >= 500 || code == 429) {
            throw new RetryableException("HTTP " + code);
        } else if (!response.isSuccessful()) {
            throw new NonRetryableException("HTTP " + code);
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

    public CreateInstanceResponse createInstance(CreateInstanceRequest request) throws IOException {
        return executeWithRetry(() -> {
            try {
                String url = baseUrl + "CreateInstance";
                RequestBody body = RequestBody.create(
                        writeValue(request),
                        MediaType.get("application/json; charset=utf-8")
                );
                Request httpRequest = new Request.Builder().url(url).post(body).build();

                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    validateResponse(response);
                    String responseBody = response.body().string();
                    return readValue(responseBody, CreateInstanceResponse.class);
                }
            }catch (IOException e) {
                exceptionCount++;
                // Lambda 内必须将受检异常转为非受检异常
                throw new RuntimeException(e);
            }
        });
    }

    // 测试用方法：获取异常计数
    public int getExceptionCount() {
        return exceptionCount;
    }

    public DescribeUsersResponse describeUsers(DescribeUsersRequest request) throws IOException {
        return executeWithRetry(() -> {
            try {
                String url = baseUrl + "DescribeUsers";
                RequestBody body = RequestBody.create(
                        writeValue(request),
                        MediaType.get("application/json; charset=utf-8")
                );
                Request httpRequest = new Request.Builder().url(url).post(body).build();

                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    validateResponse(response);
                    String responseBody = response.body().string();
                    return readValue(responseBody, DescribeUsersResponse.class);
                }
            }catch (IOException e) {
                // Lambda 内必须将受检异常转为非受检异常
                throw new RuntimeException(e);
            }
        });
    }

//    public CreateUserResponse createUser(CreateUserRequest request) throws IOException {
//        String url = baseUrl + "CreateUser";
//        RequestBody requestBody = null;
//
//        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));
//
//
//
//        // 创建请求
//        Request.Builder okHttpRequestBuilder = new Request.Builder()
//                .url(url)
//                .method("POST", requestBody);
//        Request okHttpRequest = okHttpRequestBuilder.build();
//
//        // 执行请求
//        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//
//            // 解析响应
//            String responseBody = response.body().string();
//            return objectMapper.readValue(responseBody, CreateUserResponse.class);
//        }
//    }
//    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest request) throws IOException {
//        String url = baseUrl + "DescribeInstances";
//        RequestBody requestBody = null;
//
//        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));
//
//
//
//        // 创建请求
//        Request.Builder okHttpRequestBuilder = new Request.Builder()
//                .url(url)
//                .method("POST", requestBody);
//        Request okHttpRequest = okHttpRequestBuilder.build();
//
//        // 执行请求
//        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//
//            // 解析响应
//            String responseBody = response.body().string();
//            return objectMapper.readValue(responseBody, DescribeInstancesResponse.class);
//        }
//    }

}