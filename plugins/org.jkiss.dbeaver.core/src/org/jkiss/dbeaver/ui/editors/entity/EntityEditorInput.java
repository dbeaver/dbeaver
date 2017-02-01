/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * EntityEditorInput
 */
public class EntityEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    public EntityEditorInput(@NotNull DBNDatabaseNode dbmNode)
    {
        super(dbmNode);
    }

    public EntityEditorInput(@NotNull DBNDatabaseNode dbnDatabaseNode, @Nullable DBECommandContext commandContext)
    {
        super(dbnDatabaseNode, commandContext);
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

}