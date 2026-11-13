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
package org.jkiss.dbeaver.model.datadam.mcp;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AITextUtils;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DDMcpScopeUtils {
    private DDMcpScopeUtils() {
    }

    @Nullable
    public static AIDatabaseScope getScope(
        @NotNull AIContextSettingsDataSource settings,
        @NotNull DBPDataSourceContainer connection,
        @Nullable DBCExecutionContext executionContext
    ) {
        AIUtils.updateScopeSettingsIfNeeded(settings, connection, executionContext);
        return settings.getScope();
    }

    @NotNull
    public static List<DBSObject> loadCustomEntities(
        @NotNull AIContextSettingsDataSource settings,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) {
        String[] customObjectIds = settings.getCustomObjectIds();
        if (customObjectIds == null || customObjectIds.length == 0) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        Collections.addAll(ids, customObjectIds);
        return AITextUtils.loadCustomEntities(monitor, dataSource, ids);
    }
}
