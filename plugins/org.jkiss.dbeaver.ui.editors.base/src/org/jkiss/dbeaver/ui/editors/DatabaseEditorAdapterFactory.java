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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

/**
 * DatabaseEditorAdapterFactory
 */
public class DatabaseEditorAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        DBSObject.class,
        DBSDataContainer.class,
        DBSDataManipulator.class,
        DBPDataSourceContainer.class,
        DBSDataContainer.class,
        DBPDataSourceContainer.class
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
        if (adapterType == DBPDataSourceContainer.class) {
            if (adaptableObject instanceof IDataSourceContainerProvider) {
                return adapterType.cast(((IDataSourceContainerProvider) adaptableObject).getDataSourceContainer());
            }
            if (adaptableObject instanceof IEditorPart) {
                adaptableObject = ((IEditorPart) adaptableObject) .getEditorInput();
            }
            if (adaptableObject instanceof DBPDataSourceContainer) {
                return adapterType.cast(adaptableObject);
            } else if (adaptableObject instanceof IDataSourceContainerProvider) {
                return adapterType.cast(((IDataSourceContainerProvider) adaptableObject).getDataSourceContainer());
            } else if (adaptableObject instanceof IEditorInput) {
                return adapterType.cast(EditorUtils.getInputDataSource((IEditorInput) adaptableObject));
            }
            return null;
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof IEditorPart) {
                IEditorInput editorInput = ((IEditorPart) adaptableObject).getEditorInput();
                if (editorInput instanceof IDatabaseEditorInput) {
                    DBNNode node = ((IDatabaseEditorInput) editorInput).getNavigatorNode();
                    if (node != null) {
                        DBSObject object = ((DBSWrapper)node).getObject();
                        if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                            return adapterType.cast(object);
                        }
                    }
                }
            }
        }/* else if (adaptableObject instanceof EntityEditor) {
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
        }*/

        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}
