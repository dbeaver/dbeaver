/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.ui;

import org.jkiss.code.NotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface UIServiceAuth {

    /**
     * Shows a modal dialog displaying an authorization code. Doesn't prompt anything.
     * <p>
     * <b>This is a UI-blocking call.</b>
     * <p>
     * If the user decides to cancel the request, the {@code future} will be canceled.
     * Otherwise, it's the responsibility of the caller to complete the {@code future}
     * when the request is either fulfilled or failed to close the popup.
     *
     * @param browserUrl the url to be opened in the browser
     * @param userCode   the code the user has to enter in order to authenticate
     * @param future     a completable future that is completed once the dialog is closed.
     */
    void showCodePopup(@NotNull URI browserUrl, @NotNull String userCode, @NotNull CompletableFuture<Void> future);

}
