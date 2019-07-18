package org.molkex.spring.minimalrest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ISE extends RuntimeException {
    public ISE(Throwable t) {
        super(t);
    }
}