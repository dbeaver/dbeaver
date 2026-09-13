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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

/**
 * Opens the object's editor at the "Privileges" tab for the schema/table/procedure
 * selected in the navigator.
 */
public class MySQLObjectPrivilegesHandler extends AbstractHandler {

    private static final String PRIVILEGES_EDITOR_ID = "org.jkiss.dbeaver.ext.mysql.ui.editors.MySQLObjectPrivilegesEditor"; //$NON-NLS-1$

    @Override
    public @Nullable Object execute(@NotNull ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection structuredSelection) || structuredSelection.isEmpty()) {
            return null;
        }
        Object element = structuredSelection.getFirstElement();
        DBSObject object = null;
        DBNDatabaseNode node = null;
        if (element instanceof DBNDatabaseNode dbNode) {
            node = dbNode;
            object = dbNode.getObject();
        } else if (element instanceof DBSObject dbsObject) {
            object = dbsObject;
        }
        if (!(object instanceof MySQLCatalog || object instanceof MySQLTableBase || object instanceof MySQLProcedure)) {
            return null;
        }
        if (node == null) {
            node = NavigatorHandlerObjectOpen.getNodeByObject(object);
        }
        if (node != null) {
            NavigatorHandlerObjectOpen.openEntityEditor(node, PRIVILEGES_EDITOR_ID, UIUtils.getActiveWorkbenchWindow());
        }
        return null;
    }
}
