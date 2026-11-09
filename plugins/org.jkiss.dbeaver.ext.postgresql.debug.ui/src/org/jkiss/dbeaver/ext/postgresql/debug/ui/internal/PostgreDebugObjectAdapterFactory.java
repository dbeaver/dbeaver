/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.debug.DBGDebugObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

public class PostgreDebugObjectAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBGDebugObject.class };

    private static final DBGDebugObject DEBUG_OBJECT = new DBGDebugObject() {
    };

    @Override
    @Nullable
    public <T> T getAdapter(@NotNull Object adaptableObject, @NotNull Class<T> adapterType) {
        if (adapterType == DBGDebugObject.class) {
            if (adaptableObject instanceof IEditorPart editorPart) {
                adaptableObject = editorPart.getEditorInput();
            }
            if (adaptableObject instanceof IDatabaseEditorInput dei && isPostgreProcedure(dei.getDatabaseObject())) {
                return adapterType.cast(DEBUG_OBJECT);
            }
            if (adaptableObject instanceof DBNDatabaseNode databaseNode && isPostgreProcedure(databaseNode.getObject())) {
                return adapterType.cast(DEBUG_OBJECT);
            }
        }
        return null;
    }

    private static boolean isPostgreProcedure(@Nullable DBSObject object) {
        if (object != null) {
            // An instanceof check on every active editor starts the lazy PostgreSQL model bundle.
            for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
                if (type.getName().equals("org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure")) {
                    return object instanceof PostgreProcedure;
                }
            }
        }
        return false;
    }

    @Override
    @NotNull
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
