/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * Oracle object adapter
 */
public class OracleObjectAdapter implements IAdapterFactory {

    public OracleObjectAdapter() {
    }

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (OracleSourceObject.class.isAssignableFrom(adapterType)) {
            DBSObject dbObject = null;
            if (adaptableObject instanceof DBNDatabaseNode) {
                dbObject = ((DBNDatabaseNode) adaptableObject).getObject();
            } else if (adaptableObject instanceof IDatabaseEditor) {
                dbObject = ((IDatabaseEditor) adaptableObject).getEditorInput().getDatabaseObject();
            } else if (adaptableObject instanceof DatabaseEditorInput) {
                dbObject = ((DatabaseEditorInput) adaptableObject).getDatabaseObject();
            }
            if (dbObject != null && adapterType.isAssignableFrom(dbObject.getClass())) {
                return dbObject;
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { OracleSourceObject.class, OracleSourceObjectEx.class };
    }
}
