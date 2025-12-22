package cn.xej.api.common;

import java.io.IOException;

// 可重试的异常（网络 or 服务端临时错误）
public class RetryableException extends IOException {
    public RetryableException(String message) { super(message); }
}
