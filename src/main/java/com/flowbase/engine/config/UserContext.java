package com.flowbase.engine.config;

import com.flowbase.engine.auth.dto.AuthenticatedUser;

public class UserContext {
    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();
    
    public static void set(AuthenticatedUser user) {HOLDER.set(user);}
    
    public static AuthenticatedUser get() {return HOLDER.get();}
    
    public static void clear() {HOLDER.remove();}
}
