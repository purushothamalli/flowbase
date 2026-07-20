package com.flowbase.engine.realtime.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WsAction {
    AUTHENTICATE("authenticate"), SUBSCRIBE("subscribe"), UNSUBSCRIBE("unsubscribe");
    private final String value;
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static WsAction fromValue(String text) {
        if (text == null) return null;
        for (WsAction action : WsAction.values()) {
            if (action.value.equalsIgnoreCase(text)) return action;
        }
        return null;
    }
}
