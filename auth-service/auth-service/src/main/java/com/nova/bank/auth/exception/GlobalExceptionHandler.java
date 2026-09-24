package com.nova.bank.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException methodArgumentNotValidException){
        Map<String,String> errorResponse = new HashMap<>();
        methodArgumentNotValidException.getBindingResult().getAllErrors().forEach((error)->{
            String errorMessage = error.getDefaultMessage();
            String field = ((FieldError) error).getField();

            errorResponse.put(field,errorMessage);
        });
        ResponseEntity<Map<String,String>> response=new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        return response;

    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFound userNotFound){
        return new  ResponseEntity<>(userNotFound.getMessage(),HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchException(NoSuchElementException noSuchElementException){
        return new ResponseEntity<>(noSuchElementException.getMessage(),HttpStatus.NOT_FOUND);
    }
}
