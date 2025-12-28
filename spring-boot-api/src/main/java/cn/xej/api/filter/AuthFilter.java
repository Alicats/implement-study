package cn.xej.api.filter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@Order(2) 
public class AuthFilter extends OncePerRequestFilter {

    // 假设我们知道的密钥（实际项目中应该从配置或密钥管理服务获取）
    private static final String SECRET_KEY = "123456";
    private static final String SECRET_ID = "alicat";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 使用CustomRequestWrapper包装请求，允许多次读取请求体
        CustomRequestWrapper customRequest = new CustomRequestWrapper(request);
        
        try {
            // 1. 获取Authorization头
            String authHeader = customRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("HMAC-SHA256 ")) {
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

            // 4. 获取请求信息用于计算签名
            String action = customRequest.getHeader("X-TC-Action");
            String timestamp = customRequest.getHeader("X-TC-Timestamp");
            
            if (action == null || timestamp == null) {
                sendUnauthorizedResponse(response, "Missing required headers");
                return;
            }

            // 5. 获取请求体内容（使用自定义RequestWrapper可以多次读取）
            String requestBody = customRequest.getBody();
            
            // 6. 计算期望的签名
            String expectedSignature = calculateSignature(action, timestamp, requestBody);
            log.info("Expected signature: {}", expectedSignature);
            log.info("Actual signature: {}", signature);
            
            // 7. 验证签名
            if (!signature.equals(expectedSignature)) {
                log.error("Signature verification failed");
                sendUnauthorizedResponse(response, "Invalid signature");
                return;
            }
            
            log.info("Signature verification passed");
            // 8. 认证成功，继续执行过滤器链
            filterChain.doFilter(customRequest, response);
            
        } catch (Exception e) {
            log.error("Authentication error", e);
            sendUnauthorizedResponse(response, "Authentication failed");
        } 
    }

    private Map<String, String> parseAuthHeader(String authHeader) {
        Map<String, String> params = new TreeMap<>();
        String authContent = authHeader.substring("HMAC-SHA256 ".length());
        String[] pairs = authContent.split(", ");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return params;
    }
    
    private String calculateSignature(String action, String timestamp, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        // 计算请求体哈希
        String hashedRequestPayload = sha256Hex(payload);
        // 构建签名摘要字符串
        String stringToSign = String.format("%s\n%s\n%s\n%s", "POST", action, timestamp, hashedRequestPayload);
        // 计算签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
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

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(String.format("{\"code\": \"UNAUTHORIZED\", \"message\": \"%s\"}", message));
    }
}