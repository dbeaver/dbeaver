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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;

public interface UIServiceFilterConfig {

    boolean isUserChangeable(@Nullable DBSObjectFilter filter);

    void configFilterInDialog(
        @NotNull Shell shell,
        @NotNull DBNDatabaseNode originalNode,
        @NotNull DBNDatabaseNode parentNode,
        @NotNull DBXTreeItem itemsMeta
    ) throws DBException;

    void setParentNodeFilter(
        @NotNull DBNDatabaseNode parentNode,
        @NotNull DBXTreeItem itemsMeta,
        @NotNull DBSObjectFilter currentDialogFilter
    ) throws DBException;

    void removeUserFilter(
        @NotNull DBNDatabaseNode parentNode,
        @NotNull DBXTreeItem itemsMeta
    ) throws DBException;
}
