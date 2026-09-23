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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.List;

/** Resolves the narrowest metadata container affected by a SQL object operation. */
public final class SQLMetadataRefreshTargetResolver {
    private static final Log log = Log.getLog(SQLMetadataRefreshTargetResolver.class);

    private SQLMetadataRefreshTargetResolver() {
    }

    @NotNull
    public static RefreshTarget createTarget(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLObjectOperation operation
    ) {
        return createTarget(monitor, executionContext, operation, DBUtils::refreshContextDefaultsAndReflect);
    }

    @NotNull
    public static RefreshTarget createTarget(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLObjectOperation operation,
        @NotNull ContextDefaultsRefresher defaultsRefresher
    ) {
        try {
            return doCreateTarget(monitor, executionContext, operation, defaultsRefresher);
        } catch (DBException e) {
            log.debug("Error resolving metadata refresh target", e);
            return new RefreshTarget(RefreshLevel.DATA_SOURCE, null, null);
        }
    }

    @NotNull
    private static RefreshTarget doCreateTarget(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLObjectOperation operation,
        @NotNull ContextDefaultsRefresher defaultsRefresher
    ) throws DBException {
        SQLDialect dialect = executionContext.getDataSource().getSQLDialect();
        List<String> nameParts = operation.qualifiedNameParts().stream()
            .map(name -> DBUtils.getUnQuotedNormalizedIdentifier(dialect, name, true))
            .toList();
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        DBSCatalog defaultCatalog = defaults == null ? null : defaults.getDefaultCatalog();
        DBSSchema defaultSchema = defaults == null ? null : defaults.getDefaultSchema();

        if (operation.objectKind() == SQLObjectOperation.ObjectKind.DATABASE ||
            operation.objectKind() == SQLObjectOperation.ObjectKind.CATALOG) {
            if (operation.operation() != SQLObjectOperation.Operation.ALTER) {
                return new RefreshTarget(RefreshLevel.DATA_SOURCE, null, null);
            }
            return nameParts.isEmpty() ? new RefreshTarget(RefreshLevel.DATA_SOURCE, null, null) :
                new RefreshTarget(RefreshLevel.CATALOG, nameParts.getLast(), null);
        }
        if (operation.objectKind() == SQLObjectOperation.ObjectKind.SCHEMA) {
            String catalogName = nameParts.size() > 1 ? nameParts.get(nameParts.size() - 2) :
                defaultCatalog == null ? null : defaultCatalog.getName();
            if (operation.operation() != SQLObjectOperation.Operation.ALTER) {
                return new RefreshTarget(
                    catalogName == null ? RefreshLevel.DATA_SOURCE : RefreshLevel.CATALOG,
                    catalogName,
                    null
                );
            }
            return nameParts.isEmpty() ? new RefreshTarget(RefreshLevel.DATA_SOURCE, null, null) :
                new RefreshTarget(RefreshLevel.SCHEMA, catalogName, nameParts.getLast());
        }

        String catalogName = null;
        String schemaName = null;
        if (nameParts.size() >= 3) {
            catalogName = nameParts.get(nameParts.size() - 3);
            schemaName = nameParts.get(nameParts.size() - 2);
        } else if (nameParts.size() == 2) {
            if (defaults != null && !defaults.supportsSchemaChange() && defaults.supportsCatalogChange()) {
                catalogName = nameParts.getFirst();
            } else {
                schemaName = nameParts.getFirst();
            }
        }
        if (defaults != null &&
            ((catalogName == null && defaults.supportsCatalogChange()) ||
                (schemaName == null && defaults.supportsSchemaChange()))) {
            defaultsRefresher.refresh(monitor, defaults, executionContext);
            defaultCatalog = defaults.getDefaultCatalog();
            defaultSchema = defaults.getDefaultSchema();
        }
        catalogName = catalogName != null ? catalogName : defaultCatalog == null ? null : defaultCatalog.getName();
        schemaName = schemaName != null ? schemaName : defaultSchema == null ? null : defaultSchema.getName();
        return new RefreshTarget(
            schemaName != null ? RefreshLevel.SCHEMA :
                catalogName != null ? RefreshLevel.CATALOG : RefreshLevel.DATA_SOURCE,
            catalogName,
            schemaName
        );
    }

    public enum RefreshLevel {
        DATA_SOURCE,
        CATALOG,
        SCHEMA
    }

    public record RefreshTarget(
        @NotNull RefreshLevel level,
        @Nullable String catalogName,
        @Nullable String schemaName
    ) {
    }

    @FunctionalInterface
    public interface ContextDefaultsRefresher {
        void refresh(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContextDefaults<?, ?> defaults,
            @NotNull DBCExecutionContext executionContext
        ) throws DBException;
    }
}
