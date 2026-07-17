package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.ValidationException;
import com.flowbase.engine.common.service.IdGenerator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;


@Service
@AllArgsConstructor
class CollectionDataServiceImpl implements CollectionDataService {
    private final CollectionRepository collectionRepository;
    private final CollectionDocumentRepository collectionDocumentRepository;
    private final IdGenerator idGenerator;
    
    @Override
    public CollectionDocument insertDocument(String collectionId, Map<String, Object> payload) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException("Collection with given Id not found!");
        Collection collection = collectionExists.get();
        for (CollectionField field : collection.fields()) {
            if (field.required() && (payload.get(field.name()) == null || payload.get(field.name()) instanceof String str && str.isBlank()))
                throw new ValidationException(field.name() + " is Required, but marked as empty!");
            if (payload.containsKey(field.name())) {
                boolean match = switch (field.type()) {
                    case STRING -> payload.get(field.name()) instanceof String;
                    case NUMBER -> payload.get(field.name()) instanceof Number;
                    case BOOLEAN -> payload.get(field.name()) instanceof Boolean;
                    case DATETIME -> {
                        Object val = payload.get(field.name());
                        if (val instanceof String) {
                            try {
                                Instant.parse((CharSequence) val);
                                yield true;
                            } catch (Exception e) {
                                yield false;
                            }
                        }
                        yield val instanceof Date || val instanceof Instant;
                    }
                }; if (!match)
                    throw new ValidationException("Type mismatch for: " + field.name() + " Expected Type: " + field.type() + " but received: " + payload.get(field.name())
                                                                                                                                                        .getClass());
            }
        }
        
        CollectionDocument collectionDocument = new CollectionDocument(
                this.idGenerator.generate(),
                collectionId,
                payload,
                Instant.now(),
                Instant.now()
        );
        return this.collectionDocumentRepository.save(collectionDocument);
    }
}
