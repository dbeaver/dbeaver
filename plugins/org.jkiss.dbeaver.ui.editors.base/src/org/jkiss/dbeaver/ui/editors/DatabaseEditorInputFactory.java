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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

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
    static final String TAG_CONNECTION_COLOR = "connection-color"; //$NON-NLS-1$

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
}