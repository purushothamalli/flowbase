package com.flowbase.engine.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WsResponse(String type, String status, String collectionId, String userId, String error) {
    public static WsResponse error(String message) {
        return new WsResponse("error", null, null, null, message);
    }
    
    public static WsResponse status(String status, String collectionId, String userId) {
        return new WsResponse("status", status, collectionId, userId, null);
    }
}
