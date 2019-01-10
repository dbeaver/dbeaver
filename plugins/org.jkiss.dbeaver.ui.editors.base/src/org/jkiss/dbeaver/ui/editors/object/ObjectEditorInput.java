/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.navigator.DBNDatabaseObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * ObjectEditorInput
 */
public class ObjectEditorInput extends DatabaseEditorInput<DBNDatabaseObject>
{
    public ObjectEditorInput(DBNDatabaseObject dbmNode)
    {
        super(dbmNode);
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        DBNDatabaseObject node = getNavigatorNode();
//        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
//        if (editorDescriptor != null) {
//            return editorDescriptor.getImageDescriptor();
//        } else {
            return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
//        }
    }

    @Override
    public String getToolTipText()
    {
        return getNavigatorNode().getMeta().getDescription();
    }

}