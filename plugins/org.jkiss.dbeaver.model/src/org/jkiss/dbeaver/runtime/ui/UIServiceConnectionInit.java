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
package org.jkiss.dbeaver.runtime.ui;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.List;

/** Approval for driver initialization SQL executed before a JDBC connection is available. */
public interface UIServiceConnectionInit {
    default boolean confirmInitScripts(
        @NotNull DBPDataSourceContainer container,
        @Nullable String url,
        @NotNull List<String> scripts
    ) {
        for (String script : scripts) {
            if (!confirmInitScript(container, url, script)) {
                return false;
            }
        }
        return true;
    }

    boolean confirmInitScript(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script);
}
