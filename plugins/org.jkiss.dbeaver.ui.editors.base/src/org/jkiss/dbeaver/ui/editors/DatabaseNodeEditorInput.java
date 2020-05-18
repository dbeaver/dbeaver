/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DatabaseNodeEditorInput
 */
public class DatabaseNodeEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    public DatabaseNodeEditorInput(@NotNull DBNDatabaseNode dbmNode)
    {
        super(dbmNode);
    }

    public DatabaseNodeEditorInput(@NotNull DBNDatabaseNode dbnDatabaseNode, @Nullable DBECommandContext commandContext)
    {
        super(dbnDatabaseNode, commandContext);
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
            toolTip.append(node.getNodeType());
            toolTip.append(": ");
            toolTip.append(node.getNodeName());
            toolTip.append(" \n");
            if (node instanceof DBNDataSource) {
                break;
            }
        }

        return toolTip.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DatabaseNodeEditorInput && super.equals(obj);
    }
}