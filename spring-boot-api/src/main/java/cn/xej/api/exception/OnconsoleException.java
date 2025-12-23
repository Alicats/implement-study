package cn.xej.api.exception;

import java.util.Arrays;
import java.util.List;

public class OnconsoleException extends RuntimeException {
    private final String       code;
    private final int          httpStatusCode;
    private final String       msgTemplate;
    private final List<Object> params;

    public OnconsoleException(int httpStatusCode, String code, String message, List<Object> params) {
        super(message);
        this.code = code;
        this.params = params;
        this.httpStatusCode = httpStatusCode;
        this.msgTemplate = message;
    }

    public OnconsoleException(int httpStatusCode, String code, String message, Object... params) {
        this(httpStatusCode, code, message, params == null ? null : Arrays.asList(params));
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
