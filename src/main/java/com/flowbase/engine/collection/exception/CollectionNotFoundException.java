package com.flowbase.engine.collection.exception;

public class CollectionNotFoundException extends CollectionException {
    public CollectionNotFoundException(String id) {
        super("Collection with ID '" + id + "' not found!");
    }
}
