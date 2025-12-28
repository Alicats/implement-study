package cn.xej.api.filter;

import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * RequestId过滤器，负责生成和管理requestId，实现日志链路追踪
 */
@Slf4j
@Component
@Order(1) // 设置执行顺序，确保先于认证过滤器执行
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-TC-RequestId";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 尝试从请求头获取（比如来自Nginx或上游微服务zuul）
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            
            // 2. 如果没有，则生成新的
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString().replaceAll("-", "");
            }

            // 3. 放入MDC（为了日志打印）
            MDC.put(MDC_KEY, requestId);
            
            // 4. 放入响应头（为了让SDK即使报错时也能从Header拿到）
            response.setHeader(HEADER_REQUEST_ID, requestId);
            
            // 5. 继续执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 6. 移除MDC（为了线程安全）
            MDC.remove(MDC_KEY);
        }
    }
}
