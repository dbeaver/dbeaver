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

import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.SecurityUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Local approvals for connection initialization SQL. */
public final class ConfirmedConnectionInitScriptsStore {
    public static final String FILE_NAME = "confirmed_connection_init_scripts.json";

    private static final Log log = Log.getLog(ConfirmedConnectionInitScriptsStore.class);
    private static final Object STORE_LOCK = new Object();

    private final Path path;

    public ConfirmedConnectionInitScriptsStore(@NotNull Path path) {
        this.path = path;
    }

    public boolean contains(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script) {
        return contains(container, url, List.of(script));
    }

    public boolean contains(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull List<String> scripts) {
        synchronized (STORE_LOCK) {
            return loadApprovals().contains(approvalKey(container, url, scripts));
        }
    }

    public void add(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script) {
        add(container, url, List.of(script));
    }

    public void add(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull List<String> scripts) {
        synchronized (STORE_LOCK) {
            Set<String> approvals = loadApprovals();
            approvals.add(approvalKey(container, url, scripts));
            try {
                Files.createDirectories(path.toAbsolutePath().getParent());
                try (var writer = Files.newBufferedWriter(path)) {
                    JSONUtils.PRETTY_GSON.toJson(approvals, writer);
                }
            } catch (Exception e) {
                log.error("Error saving confirmed connection initialization SQL to " + path, e);
            }
        }
    }

    @NotNull
    private Set<String> loadApprovals() {
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                Set<String> approvals = JSONUtils.GSON.fromJson(reader, TypeToken.getParameterized(Set.class, String.class).getType());
                if (approvals != null) {
                    return approvals;
                }
            } catch (Exception e) {
                log.error("Error loading confirmed connection initialization SQL from " + path, e);
            }
        }
        return new HashSet<>();
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
