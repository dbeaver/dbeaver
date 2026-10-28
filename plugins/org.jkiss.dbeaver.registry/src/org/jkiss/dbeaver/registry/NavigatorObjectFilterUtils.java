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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.auth.SMObjectType;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNObjectFilterProvider;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;

import java.util.Collection;

/**
 * Resolves personal navigator filters without putting them in the shared datasource metadata configuration.
 */
public final class NavigatorObjectFilterUtils {
    public static final DBNObjectFilterProvider FILTER_PROVIDER = (node, meta, connectionFilter) ->
        DBNObjectFilterProvider.createFilterMatcher(connectionFilter, getEffectiveUserFilter(node, meta));

    private NavigatorObjectFilterUtils() {
    }

    /** Returns the filter defined on this node, including disabled and empty filters, for editing. */
    @Nullable
    public static DBSObjectFilter getUserFilter(@NotNull DBNDatabaseNode node, @Nullable DBXTreeItem meta) {
        return resolveUserFilter(node, meta, true);
    }

    /** Returns the applicable filter, including global fallback, using the datasource's matching rules. */
    @Nullable
    public static DBSObjectFilter getEffectiveUserFilter(@NotNull DBNDatabaseNode node, @Nullable DBXTreeItem meta) {
        DBSObjectFilter filter = resolveUserFilter(node, meta, false);
        DBPDataSource dataSource = node.getDataSource();
        if (filter != null && dataSource != null) {
            filter.setCaseSensitive(dataSource.getSQLDialect().hasCaseSensitiveFiltration());
        }
        return filter;
    }

    /** @return false if the node does not support object filters */
    public static boolean setUserFilter(
        @NotNull DBNDatabaseNode node,
        @Nullable DBXTreeItem meta,
        @Nullable DBSObjectFilter filter
    ) throws DBException {
        Class<?> childClass = node.getChildrenOrFolderClass(meta);
        if (childClass == null) {
            return false;
        }
        UserDBSObjectFilterUtils.setUserObjectFilter(node.getDataSourceContainer(), childClass, node.getFilterObject(), filter);
        return true;
    }

    /** Refilters already-loaded navigator children after local or remote settings changes. */
    public static void objectSettingsChanged(
        @NotNull DBPProject project,
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        @Nullable Collection<String> settingIds
    ) {
        if (objectType != SMObjectType.datasource ||
            (settingIds != null && settingIds.stream().noneMatch(id -> id.startsWith(UserDBSObjectFilterUtils.USER_FILTER_KEY)))
        ) {
            return;
        }
        DBNDatabaseNode node = findDataSourceNode(project, objectId);
        if (node == null || node.isDisposed() || node.needsInitialization()) {
            return;
        }
        // Rebuild navigator children, not datasource configuration or metadata caches. Property getters can
        // still access the database, so never do this on the settings event thread or the UI thread.
        AbstractJob job = new AbstractJob("Update navigator filters") {
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                DBNDatabaseNode currentNode = findDataSourceNode(project, objectId);
                if (currentNode != null && !currentNode.isDisposed() && !monitor.isCanceled()) {
                    currentNode.updateChildrenOrder(true);
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(@Nullable Object family) {
                return family == NavigatorObjectFilterUtils.class;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    @Nullable
    private static DBNDatabaseNode findDataSourceNode(@NotNull DBPProject project, @NotNull String objectId) {
        DBNModel model = project.getNavigatorModel();
        if (model == null) {
            return null;
        }
        DBPDataSourceContainer dataSource = project.getDataSourceRegistry().getDataSource(objectId);
        return dataSource == null ? null : model.getNodeByObject(dataSource);
    }

    @Nullable
    private static DBSObjectFilter resolveUserFilter(
        @NotNull DBNDatabaseNode node,
        @Nullable DBXTreeItem meta,
        boolean firstMatch
    ) {
        Class<?> childClass = node.getChildrenOrFolderClass(meta);
        return childClass == null ? null : UserDBSObjectFilterUtils.getUserObjectFilter(
            node.getDataSourceContainer(), childClass, node.getFilterObject(), firstMatch);
    }
}
