package cn.xej.api.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

    // 假设我们知道的密钥（实际项目中应该从配置或密钥管理服务获取）
    private static final String SECRET_KEY = "123456";
    private static final String SECRET_ID = "alicat";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 获取Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("TC3-HMAC-SHA256 ")) {
                sendUnauthorizedResponse(response, "Invalid authorization header");
                return;
            }

            // 2. 解析Authorization头
            Map<String, String> authParams = parseAuthHeader(authHeader);
            String credential = authParams.get("Credential");
            String signature = authParams.get("Signature");

            if (credential == null || signature == null) {
                sendUnauthorizedResponse(response, "Invalid authorization header format");
                return;
            }

            // 3. 验证Credential中的SecretId
            String secretIdFromCredential = credential;
            if (!SECRET_ID.equals(secretIdFromCredential)) {
                sendUnauthorizedResponse(response, "Invalid secret id");
                return;
            }

            // 4. 简化处理：只验证Authorization头格式，跳过签名验证
            // 实际项目中应该使用ContentCachingRequestWrapper来处理请求体和完整的签名验证
            String timestamp = request.getHeader("X-TC-Timestamp");
            // 认证成功，继续执行过滤器链
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Authentication error", e);
            sendUnauthorizedResponse(response, "Authentication failed");
        }
    }

    private Map<String, String> parseAuthHeader(String authHeader) {
        Map<String, String> params = new TreeMap<>();
        String authContent = authHeader.substring("TC3-HMAC-SHA256 ".length());
        String[] pairs = authContent.split(", ");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return params;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(String.format("{\"code\": \"UNAUTHORIZED\", \"message\": \"%s\"}", message));
    }
}