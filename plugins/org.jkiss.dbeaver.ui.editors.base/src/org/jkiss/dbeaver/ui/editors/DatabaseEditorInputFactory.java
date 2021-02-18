/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class DatabaseEditorInputFactory implements IElementFactory
{
    public static final String ID_FACTORY = DatabaseEditorInputFactory.class.getName(); //$NON-NLS-1$

    static final String TAG_CLASS = "class"; //$NON-NLS-1$
    static final String TAG_PROJECT = "project"; //$NON-NLS-1$
    static final String TAG_DATA_SOURCE = "data-source"; //$NON-NLS-1$
    static final String TAG_NODE = "node"; //$NON-NLS-1$
    static final String TAG_NODE_NAME = "node-name"; //$NON-NLS-1$
    static final String TAG_ACTIVE_PAGE = "page"; //$NON-NLS-1$
    static final String TAG_ACTIVE_FOLDER = "folder"; //$NON-NLS-1$

    private static volatile boolean lookupEditor;

    public DatabaseEditorInputFactory()
    {
    }

    public static void setLookupEditor(boolean lookupEditor) {
        DatabaseEditorInputFactory.lookupEditor = lookupEditor;
    }

    @Override
    public IAdaptable createElement(IMemento memento) {
        return new DatabaseLazyEditorInput(memento);
    }

    public static void saveState(IMemento memento, DatabaseEditorInput input) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DatabaseEditorPreferences.PROP_SAVE_EDITORS_STATE)) {
            return;
        }
        final DBCExecutionContext context = input.getExecutionContext();
        if (context == null) {
            // Detached - nothing to save
            return;
        }
        if (input.getDatabaseObject() != null && !input.getDatabaseObject().isPersisted()) {
            return;
        }

        final DBNDatabaseNode node = input.getNavigatorNode();
        memento.putString(TAG_CLASS, input.getClass().getName());
        memento.putString(TAG_PROJECT, context.getDataSource().getContainer().getProject().getName());
        memento.putString(TAG_DATA_SOURCE, context.getDataSource().getContainer().getId());
        memento.putString(TAG_NODE, node.getNodeItemPath());
        memento.putString(TAG_NODE_NAME, node.getNodeName());
        if (!CommonUtils.isEmpty(input.getDefaultPageId())) {
            memento.putString(TAG_ACTIVE_PAGE, input.getDefaultPageId());
        }
        if (!CommonUtils.isEmpty(input.getDefaultFolderId())) {
            memento.putString(TAG_ACTIVE_FOLDER, input.getDefaultFolderId());
        }
    }

}