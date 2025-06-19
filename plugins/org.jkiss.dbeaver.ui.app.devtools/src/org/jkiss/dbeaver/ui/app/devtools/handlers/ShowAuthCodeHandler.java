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
package org.jkiss.dbeaver.ui.app.devtools.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ShowAuthCodeHandler extends AbstractHandler {
    private static final Log log = Log.getLog(ShowAuthCodeHandler.class);

    @Override
    public Object execute(ExecutionEvent event) {
        var service = DBWorkbench.getService(UIServiceAuth.class);
        if (service == null) {
            log.debug("Auth UI service is not available");
            return null;
        }
        service.showCodePopup(
            URI.create("https://device.sso.us-east-2.amazonaws.com/"),
            "ABCD-EF00",
            new CompletableFuture<>()
        );
        return null;
    }
}
