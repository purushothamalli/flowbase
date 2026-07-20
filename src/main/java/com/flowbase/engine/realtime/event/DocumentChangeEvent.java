package com.flowbase.engine.realtime.event;

import com.flowbase.engine.collection.domain.CollectionDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentChangeEvent extends ApplicationEvent {
    private final String action;
    private final String collectionId;
    private final CollectionDocument collectionDocument;
    
    public DocumentChangeEvent(Object source, String action, String collectionId, CollectionDocument collectionDocument) {
        super(source);
        this.action = action;
        this.collectionId = collectionId;
        this.collectionDocument = collectionDocument;
    }
}
