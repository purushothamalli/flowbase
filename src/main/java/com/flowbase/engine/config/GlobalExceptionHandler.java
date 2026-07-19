package com.flowbase.engine.config;

import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidCredentialsException;
import com.flowbase.engine.auth.exception.UserAlreadyExistsException;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.DocumentNotFoundException;
import com.flowbase.engine.collection.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleConflict(UserAlreadyExistsException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setTitle("Conflict Exception");
        return detail;
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setTitle("Invalid Credentials");
        return detail;
    }

    @ExceptionHandler(CollectionNotFoundException.class)
    public ProblemDetail handleCollectionNotFound(CollectionNotFoundException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setTitle("Collection Not Found");
        return detail;
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFound(DocumentNotFoundException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setTitle("Document Not Found");
        return detail;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setTitle("Schema Validation Error");
        return detail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException e){
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        detail.setTitle("Unauthorized Access");
        return detail;
    }
}
