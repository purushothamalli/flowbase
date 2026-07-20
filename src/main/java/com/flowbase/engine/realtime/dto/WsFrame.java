package com.flowbase.engine.realtime.dto;

public record WsFrame(WsAction action, String collectionId, String token) {}
