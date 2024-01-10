/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Objects;

/**
 * DatabaseNodeEditorInput
 */
public class DatabaseNodeEditorInput extends DatabaseEditorInput<DBNDatabaseNode> implements IUnloadableEditorInput
{
    private final String nodePath;
    private final String nodeName;

    public DatabaseNodeEditorInput(@NotNull DBNDatabaseNode dbmNode)
    {
        this(dbmNode, null);
    }

    public DatabaseNodeEditorInput(@NotNull DBNDatabaseNode dbnDatabaseNode, @Nullable DBECommandContext commandContext)
    {
        super(dbnDatabaseNode, commandContext);

        this.nodePath = dbnDatabaseNode.getNodeUri();
        this.nodeName = dbnDatabaseNode.getNodeDisplayName();
    }
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSObject.class) {
            DBSObject databaseObject = getDatabaseObject();
            return adapter.cast(databaseObject);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String getToolTipText()
    {
        StringBuilder toolTip = new StringBuilder();

        for (DBNNode node = getNavigatorNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBSFolder) {
                continue;
            }
            toolTip.append(node.getNodeTypeLabel());
            toolTip.append(": ");
            toolTip.append(node.getNodeDisplayName());
            toolTip.append(" \n");
            if (node instanceof DBNDataSource) {
                break;
            }
        }
        EditorUtils.appendProjectToolTip(toolTip, getNavigatorNode().getOwnerProject());

        return toolTip.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DatabaseNodeEditorInput && super.equals(obj);
    }

    @NotNull
    @Override
    public ILazyEditorInput unloadInput() {
        final DBCExecutionContext context = Objects.requireNonNull(getExecutionContext());
        final DBPDataSourceContainer container = context.getDataSource().getContainer();

        return new DatabaseLazyEditorInput(
            nodePath,
            nodeName,
            getDefaultPageId(),
            getDefaultFolderId(),
            container.getId(),
            getClass().getName(),
            container.getProject(),
            container,
            false
        );
    }
}