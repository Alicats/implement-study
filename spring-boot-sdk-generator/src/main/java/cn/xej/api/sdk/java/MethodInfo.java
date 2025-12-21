package cn.xej.api.sdk.java;

/**
 * 方法模型类，用于存储API方法的相关信息，供模板引擎使用
 */
public class MethodInfo {
    private String returnType;
    private String methodName;
    private String fullMapping;
    private String httpMethod;
    private String methodSignature;
    private String requestBodyCode;
    private String responseParseCode;
    private String parameters;

    // Getters and setters
    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFullMapping() {
        return fullMapping;
    }

    public void setFullMapping(String fullMapping) {
        this.fullMapping = fullMapping;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getRequestBodyCode() {
        return requestBodyCode;
    }

    public void setRequestBodyCode(String requestBodyCode) {
        this.requestBodyCode = requestBodyCode;
    }

    public String getResponseParseCode() {
        return responseParseCode;
    }

    public void setResponseParseCode(String responseParseCode) {
        this.responseParseCode = responseParseCode;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}