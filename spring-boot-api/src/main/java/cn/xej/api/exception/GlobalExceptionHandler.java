package cn.xej.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OnconsoleException.class)
    public ResponseEntity<Map<String, Object>> handleOnconsoleException(OnconsoleException e) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", e.getCode());
        responseBody.put("message", e.getMessage()); // e.getMessage()已经是格式化后的消息
        
        return new ResponseEntity<>(responseBody, HttpStatus.valueOf(e.getHttpStatusCode()));
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleOnconsoleException(MethodArgumentNotValidException e) {
        // 提取所有校验失败的字段和错误信息
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", "InvalidParameter");
        responseBody.put("message", errorMsg);
        
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }
}
