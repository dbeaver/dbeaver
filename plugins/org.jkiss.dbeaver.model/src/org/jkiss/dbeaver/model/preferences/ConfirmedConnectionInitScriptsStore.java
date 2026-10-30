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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.SecurityUtils;

import java.nio.file.Path;
import java.util.List;

/** Local approvals for connection initialization SQL. */
public final class ConfirmedConnectionInitScriptsStore {
    public static final String FILE_NAME = "confirmed_connection_init_scripts.json";

    private static final Object STORE_LOCK = new Object();

    private final ConfirmedActionsStore actions;

    public ConfirmedConnectionInitScriptsStore(@NotNull Path path) {
        actions = new ConfirmedActionsStore(path);
    }

    public boolean contains(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script) throws DBException {
        return contains(container, url, List.of(script));
    }

    public boolean contains(
        @NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull List<String> scripts
    ) throws DBException {
        synchronized (STORE_LOCK) {
            return actions.contains(approvalKey(container, url, scripts));
        }
    }

    public void add(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script) throws DBException {
        add(container, url, List.of(script));
    }

    public void add(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull List<String> scripts) throws DBException {
        synchronized (STORE_LOCK) {
            actions.add(approvalKey(container, url, scripts));
        }
    }

    @NotNull
    private String approvalKey(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull List<String> scripts) {
        return SecurityUtils.sha256Hex(JSONUtils.GSON.toJson(new Object[] {
            container.getProject().getAbsolutePath().toAbsolutePath().normalize().toString(),
            container.getId(),
            container.getDriver().getFullId(),
            url,
            scripts
        }));
    }
}
