package com.hospital.telemedicine.exception;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.hospital.telemedicine.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError fieldError = (FieldError) ex.getBindingResult().getAllErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();

        Map<String, Object> response = new HashMap<>();
        response.put("message", errorMessage);
        response.put("success", false);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "Tài khoản hoặc mật khẩu không chính xác");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }



    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "Authentication failed: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }



    // Bắt lỗi dùng sai phương thức (GET thay vì POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Phương thức không được hỗ trợ");
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED); // 405
    }

    // Bắt lỗi tổng quát để không bị 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Đã xảy ra lỗi: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidJson(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        // Phân biệt lỗi thiếu body hay sai định dạng JSON
        if (ex.getMessage().contains("Required request body is missing")) {
            response.put("message", "Body yêu cầu không được để trống");
        } else if (ex.getCause() instanceof MismatchedInputException) {
            response.put("message", "Dữ liệu không đúng định dạng JSON");
        } else {
            response.put("message", "Dữ liệu gửi lên không hợp lệ");
        }

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "UserId không hợp lệ");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // Trả về mã lỗi 400
    }
}