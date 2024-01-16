/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;

import java.util.Map;

public class AuthInfo {
    private final String authProvider;
    private transient Map<String, Object> userCredentials;

    public AuthInfo(@NotNull String authProvider, @NotNull Map<String, Object> userCredentials) {
        this.authProvider = authProvider;
        this.userCredentials = userCredentials;
    }

    public Map<String, Object> getUserCredentials() {
        return userCredentials;
    }

    public void setUserCredentials(Map<String, Object> userCredentials) {
        this.userCredentials = userCredentials;
    }

    public String getAuthProvider() {
        return authProvider;
    }
}
