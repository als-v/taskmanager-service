package com.elotech.taskmanager.domain.error;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

    public BusinessException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
