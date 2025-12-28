package cn.xej.api.exception;

import java.util.Arrays;
import java.util.List;

public class OnconsoleException extends RuntimeException {
    private final String       code;
    private final int          httpStatusCode;
    private final String       msgTemplate;
    private final List<Object> params;

    public OnconsoleException(int httpStatusCode, String code, String message, List<Object> params) {
        super(formatMessage(message, params));
        this.code = code;
        this.params = params;
        this.httpStatusCode = httpStatusCode;
        this.msgTemplate = message;
    }

    public OnconsoleException(int httpStatusCode, String code, String message, Object... params) {
        this(httpStatusCode, code, message, params == null ? null : Arrays.asList(params));
    }

    /**
     * 将消息模板中的 `{}` 占位符替换为实际参数值
     */
    private static String formatMessage(String template, List<Object> params) {
        if (template == null) {
            return null;
        }
        if (params == null || params.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Object param : params) {
            String paramStr = param == null ? "null" : param.toString();
            // 匹配带反引号的占位符 `{}`，使用\Q和\E将其作为字面量处理
            result = result.replaceFirst("\\Q`{}`\\E", paramStr);
        }
        return result;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMsgTemplate() {
        return msgTemplate;
    }

    public List<Object> getParams() {
        return params;
    }
}
