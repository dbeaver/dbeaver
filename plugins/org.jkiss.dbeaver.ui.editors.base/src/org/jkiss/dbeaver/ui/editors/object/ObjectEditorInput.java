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
package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * ObjectEditorInput
 */
public class ObjectEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{

    private DBXTreeObject editorMeta;

    public ObjectEditorInput(@NotNull DBNDatabaseObject dbmNode)
    {
        super(dbmNode);
    }

    public ObjectEditorInput(@NotNull DBNDatabaseNode dbmNode, @NotNull  DBXTreeObject meta)
    {
        super(dbmNode);
        this.editorMeta = meta;
    }

    public DBXTreeObject getEditorMeta() {
        if (editorMeta != null) {
            return editorMeta;
        } else {
            return ((DBXTreeObject)getNavigatorNode().getMeta());
        }
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        DBNDatabaseNode node = getNavigatorNode();
//        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
//        if (editorDescriptor != null) {
//            return editorDescriptor.getImageDescriptor();
//        } else {
            return DBeaverIcons.getImageDescriptor(getEditorMeta().getDefaultIcon());
//        }
    }

    @Override
    public String getToolTipText()
    {
        return getEditorMeta().getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ObjectEditorInput && super.equals(obj) && getEditorMeta() == ((ObjectEditorInput) obj).getEditorMeta();
    }
}