package com.checkup.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 404 for anything that doesn't exist OR that the current user isn't allowed to see.
 * Same answer for both on purpose, so ids of other people's records can't be probed.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException() {
        super("Not found");
    }
}
