/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;

import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Shows the device authorization code to the user.
 */
public interface ClickhouseAuthPrompt {

    /**
     * Default prompt, shows a modal dialog with the verification url and the user code.
     */
    ClickhouseAuthPrompt DEFAULT = (verificationUri, userCode, cancellation) -> {
        UIServiceAuth authService = DBWorkbench.getService(UIServiceAuth.class);
        if (authService == null) {
            throw new DBException("Browser authentication is available only in desktop applications");
        }
        authService.showCodePopup(verificationUri, userCode, cancellation);
    };

    /**
     * Shows the verification url and the user code to the user.
     * The prompt must be closed when the passed future completes.
     */
    void showUserCode(
        @NotNull URI verificationUri,
        @NotNull String userCode,
        @NotNull CompletableFuture<Void> cancellation
    ) throws DBException;

    /**
     * Opens the authorization page in the user's browser.
     * Used by the authorization code flow, where there is no code to type in.
     */
    default void openBrowser(@NotNull URI authorizationUri) throws DBException {
        String command;
        if (RuntimeUtils.isMacOS()) {
            command = "open";
        } else if (RuntimeUtils.isWindows()) {
            command = "explorer";
        } else {
            command = "xdg-open";
        }
        try {
            new ProcessBuilder(command, authorizationUri.toString()).start();
        } catch (Exception e) {
            // Failing silently would leave the user staring at a progress bar until the login times out
            throw new DBException(
                "Cannot open the browser automatically. Open this address to sign in: " + authorizationUri, e);
        }
    }
}
