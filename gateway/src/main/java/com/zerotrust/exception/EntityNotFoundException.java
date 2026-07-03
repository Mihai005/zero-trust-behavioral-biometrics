package com.zerotrust.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entityName, Long entityId) {
        super(String.format("%s with id %s not found", entityName, entityId.toString()));
    }

    public EntityNotFoundException(String entityName, String name) {
        super(String.format("%s with name %s not found", entityName, name));
    }
}
