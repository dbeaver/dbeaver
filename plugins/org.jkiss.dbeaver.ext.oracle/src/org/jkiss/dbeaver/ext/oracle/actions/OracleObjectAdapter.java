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
package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJob;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * Oracle object adapter
 */
public class OracleObjectAdapter implements IAdapterFactory {

    public OracleObjectAdapter() {
    }

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (DBSObject.class.isAssignableFrom(adapterType)) {
            DBSObject dbObject = null;
            if (adaptableObject instanceof DBNDatabaseNode) {
                dbObject = ((DBNDatabaseNode) adaptableObject).getObject();
            } else if (adaptableObject instanceof IDatabaseEditor) {
                dbObject = ((IDatabaseEditor) adaptableObject).getEditorInput().getDatabaseObject();
            } else if (adaptableObject instanceof DatabaseEditorInput) {
                dbObject = ((DatabaseEditorInput) adaptableObject).getDatabaseObject();
            } else if (adaptableObject instanceof DBNDatabaseItem) {
                dbObject = ((DBNDatabaseItem) adaptableObject).getObject();
            }
            if (dbObject != null && adapterType.isAssignableFrom(dbObject.getClass())) {
                return adapterType.cast(dbObject);
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { OracleSourceObject.class, OracleProcedurePackaged.class, DBPScriptObjectExt.class, OracleSchedulerJob.class };
    }
}
