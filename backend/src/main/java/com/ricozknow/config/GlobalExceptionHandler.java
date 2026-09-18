// package com.ricozknow.config;

// import com.ricozknow.agent.AgentArticleNotFoundException;
// import com.ricozknow.article.ArticleNotFoundException;
// import com.ricozknow.article.InvalidArticleContentException;
// import com.ricozknow.article.InvalidArticleStateException;
// import com.ricozknow.auth.AuthException;
// import com.ricozknow.category.CategoryNotFoundException;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.MethodArgumentNotValidException;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;

// import java.time.Instant;
// import java.util.LinkedHashMap;
// import java.util.Map;

// @RestControllerAdvice
// public class GlobalExceptionHandler {

//     @ExceptionHandler({ArticleNotFoundException.class, CategoryNotFoundException.class, AgentArticleNotFoundException.class})
//     public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage()));
//     }

//     @ExceptionHandler({InvalidArticleStateException.class, InvalidArticleContentException.class})
//     public ResponseEntity<Map<String, Object>> handleInvalidArticleState(RuntimeException ex) {
//         return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage()));
//     }

//     @ExceptionHandler(AuthException.class)
//     public ResponseEntity<Map<String, Object>> handleAuth(AuthException ex) {
//         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, ex.getMessage()));
//     }

//     @ExceptionHandler(MethodArgumentNotValidException.class)
//     public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//         String message = ex.getBindingResult().getFieldErrors().stream()
//                 .findFirst()
//                 .map(err -> err.getField() + " " + err.getDefaultMessage())
//                 .orElse("Validation failed");
//         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, message));
//     }

//     @ExceptionHandler(IllegalStateException.class)
//     public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
//         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, ex.getMessage()));
//     }

//     private Map<String, Object> body(HttpStatus status, String message) {
//         Map<String, Object> body = new LinkedHashMap<>();
//         body.put("timestamp", Instant.now().toString());
//         body.put("status", status.value());
//         body.put("error", status.getReasonPhrase());
//         body.put("message", message);
//         return body;
//     }
// }

package com.ricozknow.config;

import com.ricozknow.agent.AgentArticleNotFoundException;
import com.ricozknow.article.ArticleNotFoundException;
import com.ricozknow.article.InvalidArticleContentException;
import com.ricozknow.article.InvalidArticleStateException;
import com.ricozknow.auth.AuthException;
import com.ricozknow.category.CategoryNotFoundException;
import com.ricozknow.common.TooManyRequestsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ArticleNotFoundException.class, CategoryNotFoundException.class, AgentArticleNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler({InvalidArticleStateException.class, InvalidArticleContentException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidArticleState(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}