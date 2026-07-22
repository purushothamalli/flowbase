package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.query.CompiledQuery;
import com.flowbase.engine.collection.query.QueryCompiler;
import com.flowbase.engine.collection.query.QueryContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentQueryExecutor {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryCompiler queryCompiler;
    private final ObjectMapper objectMapper;

    public List<CollectionDocument> queryDocuments(Collection collection, QueryContext queryContext) {
        CompiledQuery compiled = this.queryCompiler.compile(collection, queryContext);
        StringBuilder sql = new StringBuilder("SELECT ID, COLLECTION_ID, DATA, CREATED_AT, UPDATED_AT FROM COLLECTION_DOCUMENTS WHERE " + compiled.sql());
        Map<String, Object> paramMap = new HashMap<>(compiled.params());
        if (!queryContext.sortBy().isEmpty()) {
            String sortQuery = queryContext.sortBy().trim();
            String sortOrder = "ASC";
            if (sortQuery.startsWith("-")) {
                sortOrder = "DESC";
                sortQuery = sortQuery.substring(1);
            }
            if (sortQuery.equals("createdAt") || sortQuery.equals("updatedAt")) {
                sql.append(" ORDER BY ")
                   .append(sortQuery.equals("createdAt") ? "created_at " : "updated_at ")
                   .append(sortOrder);
            } else {
                String finalSortQuery = sortQuery;
                boolean match = collection.fields()
                                           .stream()
                                           .anyMatch(collectionField -> collectionField.name().equals(finalSortQuery));
                if (match) {
                    sql.append(" ORDER BY DATA ->> '").append(sortQuery).append("' ").append(sortOrder);
                }
            }
        }
        if (queryContext.limit() > 0) {
            sql.append(" LIMIT :limit_const");
            paramMap.put("limit_const", queryContext.limit());
        }
        if (queryContext.offset() > 0) {
            sql.append(" OFFSET :offset_const");
            paramMap.put("offset_const", queryContext.offset());
        }
        return this.jdbcTemplate.query(sql.toString(), paramMap, (rs, rowNum) -> {
            try {
                return getCollectionDocument(rs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map document row: ", e);
            }
        });
    }

    public List<CollectionDocument> searchDocuments(String collectionId, String searchQuery, int limit, int offset) {
        int safeLimit = limit <= 0 ? 20 : Math.clamp(limit, 1, 100);
        int safeOffset = Math.max(0, offset);
        String sql = "SELECT id, collection_id, data, created_at, updated_at, " +
                     " ts_rank(tsv_document, query) as rank " +
                     "FROM collection_documents, websearch_to_tsquery('english', :search_query) query " +
                     "WHERE collection_id = :collectionId_const " +
                     "  AND tsv_document @@ query " +
                     "ORDER BY rank DESC " +
                     "LIMIT :limit_const OFFSET :offset_const";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("collectionId_const", collectionId);
        paramMap.put("search_query", searchQuery);
        paramMap.put("limit_const", safeLimit);
        paramMap.put("offset_const", safeOffset);
        return this.jdbcTemplate.query(sql, paramMap, (rs, rowNum) -> {
            try {
                return getCollectionDocument(rs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map document row: ", e);
            }
        });
    }

    @NonNull
    private CollectionDocument getCollectionDocument(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String collId = rs.getString("collection_id");
        String rawData = rs.getString("data");
        try {
            Map<String, Object> data = this.objectMapper.readValue(rawData, new TypeReference<Map<String, Object>>() {});
            Instant createdAt = rs.getTimestamp("created_at").toInstant();
            Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
            return new CollectionDocument(id, collId, data, createdAt, updatedAt);
        } catch (Exception e) {
            throw new SQLException("Failed to parse data payload", e);
        }
    }
}
