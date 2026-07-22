package com.flowbase.engine.collection.service;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.exception.AclDeniedException;
import com.flowbase.engine.config.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentAclService {
    private final AclEvaluatorService aclEvaluatorService;

    public void evaluateWriteAcl(Collection collection, Map<String, Object> payload) {
        AuthenticatedUser user = UserContext.get();
        String userId = user != null ? user.id() : null;
        String userRole = user != null ? user.role().name() : null;
        AclAuthContext aclAuthContext = new AclAuthContext(
            userId != null ? userId : "anonymous", 
            userRole != null ? userRole : "anonymous"
        );
        if (!this.aclEvaluatorService.evaluate(collection.writeRule(), payload, aclAuthContext)) {
            throw new AclDeniedException("Write access denied by ACL policy.");
        }
    }

    public List<CollectionDocument> filterReadAcl(Collection collection, List<CollectionDocument> documents) {
        if (collection.readRule() == null || collection.readRule().isEmpty() || documents.isEmpty()) {
            return documents;
        }
        AuthenticatedUser currentUser = UserContext.get();
        String userId = currentUser != null ? currentUser.id() : null;
        String userRole = currentUser != null && currentUser.role() != null ? currentUser.role().name() : null;
        AclAuthContext aclAuthContext = new AclAuthContext(
            userId != null ? userId : "anonymous", 
            userRole != null ? userRole : "anonymous"
        );
        return documents.stream()
                        .filter(doc -> this.aclEvaluatorService.evaluate(collection.readRule(), doc.data(), aclAuthContext))
                        .toList();
    }
}
