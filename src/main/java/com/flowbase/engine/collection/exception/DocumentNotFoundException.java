package com.flowbase.engine.collection.exception;

public class DocumentNotFoundException extends CollectionException {
    public DocumentNotFoundException(String id) {
        super("Document with ID '" + id + "' not found!");
    }
}
