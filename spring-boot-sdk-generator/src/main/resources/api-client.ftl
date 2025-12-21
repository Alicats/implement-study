package ${packageName};

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import ${packageName}.models.*;

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

    <#list methods as method>
    public ${method.returnType} ${method.methodName}(${method.parameters}) throws IOException {
        String url = baseUrl + "${method.fullMapping}";
        RequestBody requestBody = null;

        ${method.requestBodyCode}

        // 创建请求
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url)
                .method("${method.httpMethod}", requestBody);
        Request okHttpRequest = okHttpRequestBuilder.build();

        // 执行请求
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析响应
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, ${method.returnType}.class);
        }
    }
    </#list>
}
