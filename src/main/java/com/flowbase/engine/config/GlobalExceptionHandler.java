package com.flowbase.engine.config;

import com.flowbase.engine.auth.exception.InvalidCredentialsException;
import com.flowbase.engine.auth.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleConflict(UserAlreadyExistsException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e.getMessage());
        detail.setTitle("Conflict exception");
        return detail;
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage());
        detail.setTitle("Invalid credentials!");
        return detail;
    }
}
