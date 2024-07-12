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
package org.jkiss.dbeaver.ext.postgresql.debug.ui.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.debug.DBGDebugObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.ui.editors.PostgreSourceViewEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

public class PostgreDebugObjectAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBGDebugObject.class };

    private static final DBGDebugObject DEBUG_OBJECT = new DBGDebugObject() {
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGDebugObject.class) {
            if (adaptableObject instanceof IEditorPart editorPart) {
                adaptableObject = editorPart.getEditorInput();
            }
            if (adaptableObject instanceof PostgreSourceViewEditor viewEditor && viewEditor.getSourceObject() instanceof PostgreProcedure) {
                return adapterType.cast(DEBUG_OBJECT);
            }
            if (adaptableObject instanceof IDatabaseEditorInput dei && dei.getDatabaseObject() instanceof PostgreProcedure) {
                return adapterType.cast(DEBUG_OBJECT);
            }
            if (false && adaptableObject instanceof INavigatorModelView nmv) {
                Viewer navigatorViewer = nmv.getNavigatorViewer();
                if (navigatorViewer != null) {
                    adaptableObject = ((IStructuredSelection) navigatorViewer.getSelection()).getFirstElement();
                }
            }
            if (adaptableObject instanceof DBNDatabaseNode databaseNode && databaseNode.getObject() instanceof PostgreProcedure) {
                return adapterType.cast(DEBUG_OBJECT);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
