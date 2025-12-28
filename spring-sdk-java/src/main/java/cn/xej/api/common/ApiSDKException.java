package cn.xej.api.common;

public class ApiSDKException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String requestId;

    private String errorCode;

    public ApiSDKException(String message, Throwable cause) {
        super(message, cause);
        this.requestId = "";
        this.errorCode = "";
    }

    public ApiSDKException(String message) {
        this(message, "", "");
    }

    public ApiSDKException(String message, String requestId, String errorCode) {
        super(message);
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    public ApiSDKException(String message, String requestId, String errorCode, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "ApiSDKException{" +
                "message='" + getMessage() + "'" +
                ", errorCode='" + errorCode + "'" +
                ", requestId='" + requestId + "'" +
                '}';
    }
}
