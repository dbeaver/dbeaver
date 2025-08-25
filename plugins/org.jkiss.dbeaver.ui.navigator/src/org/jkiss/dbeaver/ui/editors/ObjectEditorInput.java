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

import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;

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

    @Nullable
    public DBXTreeObject getEditorMeta() {
        if (editorMeta != null) {
            return editorMeta;
        }
        DBNDatabaseNode node = getNavigatorNode();
        if (node != null && node.getMeta() instanceof DBXTreeObject meta) {
            return meta;
        }
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        DBXTreeObject meta = getEditorMeta();
        return meta == null ? null : DBeaverIcons.getImageDescriptor(meta.getDefaultIcon());
    }

    @Override
    public String getToolTipText() {
        DBXTreeObject meta = getEditorMeta();
        return meta == null ? "" : meta.getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ObjectEditorInput && super.equals(obj) && getEditorMeta() == ((ObjectEditorInput) obj).getEditorMeta();
    }
}