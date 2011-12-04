/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IFolderedPart;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

/**
 * DatabaseEditorAdapterFactory
 */
public class DatabaseEditorAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { DBSObject.class, DBSDataContainer.class, DBSDataSourceContainer.class, SQLEditorBase.class };

    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == DBSDataSourceContainer.class) {
            if (adaptableObject instanceof IEditorPart) {
                adaptableObject = ((IEditorPart) adaptableObject) .getEditorInput();
            }
            if (adaptableObject instanceof DBSDataSourceContainer) {
                return adaptableObject;
            }
            if (adaptableObject instanceof IDataSourceContainerProvider) {
                return ((IDataSourceContainerProvider)adaptableObject).getDataSourceContainer();
            }
            return null;
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof IEditorPart) {
                IEditorInput editorInput = ((IEditorPart) adaptableObject).getEditorInput();
                if (editorInput instanceof IDatabaseEditorInput) {
                    DBNNode node = ((IDatabaseEditorInput) editorInput).getTreeNode();
                    if (node instanceof DBSWrapper) {
                        DBSObject object = ((DBSWrapper)node).getObject();
                        if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                            return object;
                        }
                    }
                }
            }
        } else if (adaptableObject instanceof EntityEditor) {
            IEditorPart activeEditor = ((EntityEditor) adaptableObject).getActiveEditor();
            if (activeEditor != null) {
                if (adapterType.isAssignableFrom(activeEditor.getClass())) {
                    return activeEditor;
                }
                if (activeEditor instanceof IFolderedPart) {
                    Object activeFolder = ((IFolderedPart) activeEditor).getActiveFolder();
                    if (activeFolder != null) {
                        if (adapterType.isAssignableFrom(activeFolder.getClass())) {
                            return activeEditor;
                        }
                        if (activeFolder instanceof IAdaptable) {
                            return ((IAdaptable) activeFolder).getAdapter(adapterType);
                        }
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
