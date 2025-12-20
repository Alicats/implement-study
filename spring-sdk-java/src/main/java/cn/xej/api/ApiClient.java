package cn.xej.api;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import cn.xej.api.models.*;

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

    public CreateUserResponse createUser(CreateUserRequest request) throws IOException {
        String url = baseUrl + "CreateUser";
        RequestBody requestBody = null;
        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));

        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, CreateUserResponse.class);
        }
    }

    public DescribeUsersResponse describeUsers(DescribeUsersRequest request) throws IOException {
        String url = baseUrl + "DescribeUsers";
        RequestBody requestBody = null;
        requestBody = RequestBody.create(objectMapper.writeValueAsString(request), MediaType.parse("application/json; charset=utf-8"));

        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("POST", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, DescribeUsersResponse.class);
        }
    }

}