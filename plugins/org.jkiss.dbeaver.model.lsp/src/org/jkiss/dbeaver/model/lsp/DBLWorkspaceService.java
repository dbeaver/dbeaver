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
package org.jkiss.dbeaver.model.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

class DBLWorkspaceService implements WorkspaceService {
    private static final Log log = Log.getLog(DBLWorkspaceService.class);

    @Override
    public void didChangeConfiguration(@NotNull DidChangeConfigurationParams params) {
        log.debug(params);
    }

    @Override
    public void didChangeWatchedFiles(@NotNull DidChangeWatchedFilesParams params) {
        log.debug(params);
    }
}
