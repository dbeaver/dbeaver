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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerRefresh;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSchemaHandler extends AbstractHandler {

    protected SQLEditor getEditor(ExecutionEvent event) {
        return RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
    }

    protected DBCExecutionContextDefaults<?, ?> getExecutionContext(SQLEditor editor) {
        return (DBCExecutionContextDefaults<?, ?>) editor.getExecutionContext();
    }

    protected DBNModel getNavigatorModel(SQLEditor editor) {
        return editor.getProject().getNavigatorModel();
    }

    protected List<DBNDatabaseNode> getSchemaNodes(SQLEditor editor, DBSCatalog catalog) throws DBException {
        var schemaNodes = new ArrayList<DBNDatabaseNode>();
        var monitor = new VoidProgressMonitor();
        for (DBSObject child : catalog.getChildren(monitor)) {
            if (child instanceof DBSSchema schema) {
                var navigatorModel = getNavigatorModel(editor);
                if (navigatorModel != null) {
                    var schemaNode = navigatorModel.findNode(schema);
                    if (schemaNode != null) {
                        schemaNodes.add(schemaNode);
                    }
                }
            }
        }
        return schemaNodes;
    }

    protected void refreshNodes(List<DBNDatabaseNode> nodes) {
        if (!nodes.isEmpty()) {
            NavigatorHandlerRefresh.refreshNavigator(nodes);
        }
    }
}
