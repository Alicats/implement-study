package cn.xej.api;

import cn.xej.api.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

/**
 * 统一API客户端
 */
public class ApiClient {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public CreateInstanceResponse createInstance(CreateInstanceRequest request) throws IOException {
        String url = baseUrl + "CreateInstance";
        RequestBody requestBody = null;

        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));



        // 创建请求
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        // 执行请求
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析响应
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, CreateInstanceResponse.class);
        }
    }
    public DescribeUsersResponse describeUsers(DescribeUsersRequest request) throws IOException {
        String url = baseUrl + "DescribeUsers";
        RequestBody requestBody = null;

        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));



        // 创建请求
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        // 执行请求
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析响应
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, DescribeUsersResponse.class);
        }
    }
    public CreateUserResponse createUser(CreateUserRequest request) throws IOException {
        String url = baseUrl + "CreateUser";
        RequestBody requestBody = null;

        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));



        // 创建请求
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        // 执行请求
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析响应
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, CreateUserResponse.class);
        }
    }
    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest request) throws IOException {
        String url = baseUrl + "DescribeInstances";
        RequestBody requestBody = null;

        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));



        // 创建请求
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        // 执行请求
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析响应
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, DescribeInstancesResponse.class);
        }
    }

}