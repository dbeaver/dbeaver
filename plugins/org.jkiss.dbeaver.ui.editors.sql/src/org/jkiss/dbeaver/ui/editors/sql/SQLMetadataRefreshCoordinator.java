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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLMetadataRefreshTargetResolver.RefreshTarget;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerRefresh;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

final class SQLMetadataRefreshCoordinator {
    private static final Log log = Log.getLog(SQLMetadataRefreshCoordinator.class);

    private SQLMetadataRefreshCoordinator() {
    }

    static void refresh(
        @NotNull DBCExecutionContext executionContext,
        @NotNull Set<RefreshTarget> refreshTargets,
        @Nullable Consumer<Set<RefreshTarget>> completionHandler
    ) {
        DBPDataSourceContainer dataSourceContainer = executionContext.getDataSource().getContainer();
        if (!executionContext.isConnected() || !dataSourceContainer.isConnected()) {
            return;
        }
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    Map<DBNDatabaseNode, Set<RefreshTarget>> nodeTargets = new LinkedHashMap<>();
                    Set<RefreshTarget> directlyRefreshedTargets = new LinkedHashSet<>();
                    boolean directlyRefreshed = false;
                    for (RefreshTarget target : refreshTargets) {
                        DBSObject refreshTarget = resolveTarget(monitor, executionContext, target);
                        if (refreshTarget == null) {
                            continue;
                        }
                        DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, refreshTarget, true);
                        if (node == null || node.isDisposed()) {
                            if (refreshTarget instanceof DBPRefreshableObject refreshableObject) {
                                refreshableObject.refreshObject(monitor);
                                directlyRefreshed = true;
                                directlyRefreshedTargets.add(target);
                            }
                        } else {
                            nodeTargets.computeIfAbsent(node, key -> new LinkedHashSet<>()).add(target);
                        }
                    }
                    notifyRefreshed(completionHandler, directlyRefreshedTargets);
                    List<DBNDatabaseNode> nodes = nodeTargets.keySet().stream()
                        .map(node -> Map.entry(node, getNodeDepth(node)))
                        .sorted(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .toList();
                    if (!nodes.isEmpty()) {
                        NavigatorHandlerRefresh.refreshNavigator(nodes, (refreshMonitor, refreshedNodes) -> {
                            Set<RefreshTarget> successfullyRefreshedTargets = new LinkedHashSet<>();
                            for (Map.Entry<DBNDatabaseNode, Set<RefreshTarget>> entry : nodeTargets.entrySet()) {
                                for (DBNNode refreshedNode : refreshedNodes) {
                                    if (entry.getKey() == refreshedNode || entry.getKey().isChildOf(refreshedNode)) {
                                        successfullyRefreshedTargets.addAll(entry.getValue());
                                        break;
                                    }
                                }
                            }
                            notifyRefreshed(completionHandler, successfullyRefreshedTargets);
                            if (!executionContext.isConnected() || !dataSourceContainer.isConnected()) {
                                return;
                            }
                            try {
                                refreshContextDefaults(refreshMonitor, executionContext);
                            } catch (DBException e) {
                                log.debug("Error updating execution context after metadata refresh", e);
                            }
                        });
                    }
                    if (directlyRefreshed) {
                        refreshContextDefaults(monitor, executionContext);
                        dataSourceContainer.fireEvent(new DBPEvent(
                            DBPEvent.Action.OBJECT_UPDATE,
                            dataSourceContainer,
                            DBPEvent.METADATA_REFRESH
                        ));
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                SQLEditorMessages.sql_editor_metadata_refresh_error_title,
                SQLEditorMessages.sql_editor_metadata_refresh_error_message,
                e.getTargetException()
            );
        }
    }

    private static void notifyRefreshed(
        @Nullable Consumer<Set<RefreshTarget>> completionHandler,
        @NotNull Set<RefreshTarget> refreshTargets
    ) {
        if (completionHandler != null && !refreshTargets.isEmpty()) {
            completionHandler.accept(Set.copyOf(refreshTargets));
        }
    }

    @Nullable
    private static DBSObject resolveTarget(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull RefreshTarget target
    ) throws DBException {
        DBSObjectContainer dataSourceContainer = DBUtils.getAdapter(
            DBSObjectContainer.class,
            executionContext.getDataSource()
        );
        String catalogName;
        switch (target) {
            case RefreshTarget.DataSource ignored -> {
                return dataSourceContainer;
            }
            case RefreshTarget.Catalog catalog -> catalogName = catalog.catalogName();
            case RefreshTarget.Schema schema -> catalogName = schema.catalogName();
        }
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        DBSCatalog defaultCatalog = defaults == null ? null : defaults.getDefaultCatalog();
        DBSObject catalog = defaultCatalog;
        if (catalogName != null && (defaultCatalog == null || !catalogName.equals(defaultCatalog.getName()))) {
            catalog = dataSourceContainer == null ? null : dataSourceContainer.getChild(monitor, catalogName);
        }
        if (target instanceof RefreshTarget.Catalog) {
            return catalog != null ? catalog : dataSourceContainer;
        }
        if (catalogName != null && catalog == null) {
            return dataSourceContainer;
        }
        String schemaName = ((RefreshTarget.Schema) target).schemaName();
        DBSSchema defaultSchema = defaults == null ? null : defaults.getDefaultSchema();
        if (defaultSchema != null && schemaName.equals(defaultSchema.getName()) &&
            (catalogName == null || catalog == defaultCatalog)) {
            return defaultSchema;
        }
        DBSObjectContainer schemaContainer = catalog instanceof DBSObjectContainer objectContainer ?
            objectContainer : dataSourceContainer;
        DBSObject schema = schemaContainer == null ? null : schemaContainer.getChild(monitor, schemaName);
        return schema != null ? schema : catalog != null ? catalog : dataSourceContainer;
    }

    private static void refreshContextDefaults(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext
    ) throws DBException {
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        if (defaults != null) {
            DBUtils.refreshContextDefaultsAndReflect(monitor, defaults, executionContext);
        }
    }

    private static int getNodeDepth(@NotNull DBNNode node) {
        int depth = 0;
        while ((node = node.getParentNode()) != null) {
            depth++;
        }
        return depth;
    }

}
