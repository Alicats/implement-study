package cn.xej.api.common;

import java.io.IOException;

// 不可重试的异常（客户端错误）
public class NonRetryableException extends IOException {
    public NonRetryableException(String message) { super(message); }
}
