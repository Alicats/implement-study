package cn.xej.api.filter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 自定义RequestWrapper，允许多次读取请求体
 */
public class CustomRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public CustomRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 读取请求体并保存到字节数组中
        try (InputStream inputStream = request.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            body = baos.toByteArray();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 返回一个新的ServletInputStream，使用保存的字节数组
        return new CustomServletInputStream(new ByteArrayInputStream(body));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // 返回一个新的BufferedReader，使用保存的字节数组
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 获取请求体内容
     */
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * 自定义ServletInputStream，使用ByteArrayInputStream作为数据源
     */
    private static class CustomServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CustomServletInputStream(ByteArrayInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 不支持异步读取
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}