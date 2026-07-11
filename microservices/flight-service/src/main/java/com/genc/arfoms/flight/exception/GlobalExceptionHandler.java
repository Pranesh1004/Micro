package com.genc.arfoms.flight.exception;

import com.genc.arfoms.flight.dto.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoDataFoundException.class)
    public ResponseEntity<ResponseData<Object>> handleNoDataFoundException(NoDataFoundException e) {
        logger.warn("No Data Found", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ResponseData<>(null, false, e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseData<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid Argument", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseData<>(null, false, e.getMessage()));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ResponseData<Object>> handleNoSuchElementException(java.util.NoSuchElementException e) {
        logger.warn("Element not found", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ResponseData<>(null, false, e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseData<Object>> handleIllegalStateException(IllegalStateException e) {
        logger.warn("Invalid State", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ResponseData<>(null, false, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData<Object>> handleGeneric(Exception e) {
        logger.error("Internal Server Error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseData<>(null, false, "Internal Server Error : " + e.getMessage()));
    }
}
