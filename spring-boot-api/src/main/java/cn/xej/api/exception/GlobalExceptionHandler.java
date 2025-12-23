package cn.xej.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OnconsoleException.class)
    public ResponseEntity<Map<String, Object>> handleOnconsoleException(OnconsoleException e) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", e.getCode());
        responseBody.put("message", e.getMessage());
        responseBody.put("httpStatusCode", e.getHttpStatusCode());
        responseBody.put("msgTemplate", e.getMsgTemplate());
        responseBody.put("params", e.getParams());
        
        return new ResponseEntity<>(responseBody, HttpStatus.valueOf(e.getHttpStatusCode()));
    }
}
