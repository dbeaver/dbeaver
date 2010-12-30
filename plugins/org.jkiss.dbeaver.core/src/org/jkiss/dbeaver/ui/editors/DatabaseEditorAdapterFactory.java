/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DatabaseEditorAdapterFactory
 */
public class DatabaseEditorAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { DBSObject.class, DBSDataContainer.class, DBSDataSourceContainer.class };

    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == DBSDataSourceContainer.class) {
            if (adaptableObject instanceof DBSDataSourceContainer) {
                return adaptableObject;
            }
            if (adaptableObject instanceof IDataSourceContainerProvider) {
                return ((IDataSourceContainerProvider)adaptableObject).getDataSourceContainer();
            }
            if (adaptableObject instanceof IDatabaseEditor) {
                DBNNode node = ((IDatabaseEditor) adaptableObject).getEditorInput().getTreeNode();
                if (node != null) {
                    DBSObject dbsObject = node.getObject();
                    if (dbsObject != null) {
                        DBPDataSource dataSource = dbsObject.getDataSource();
                        if (dataSource != null) {
                            return dataSource.getContainer();
                        }
                    }
                }
            }
            return null;
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof IDatabaseEditor) {
                DBNNode node = ((IDatabaseEditor) adaptableObject).getEditorInput().getTreeNode();
                if (node != null) {
                    DBSObject object = node.getObject();
                    if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                        return object;
                    }
                }
            }
        }
        return null;
    }

    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}
