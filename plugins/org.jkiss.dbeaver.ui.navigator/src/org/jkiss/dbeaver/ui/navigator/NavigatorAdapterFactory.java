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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;

/**
 * Navigator AdapterFactory
 */
public class NavigatorAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        DBPNamedObject.class,
        DBPQualifiedObject.class,
        DBPObject.class,
        DBSObject.class,
        DBSEntity.class,
        DBSDataContainer.class,
        DBSDataManipulator.class,
        DBSObjectContainer.class,
        DBSStructContainer.class,
        DBPDataSourceContainer.class,
        IPropertySource.class,
        IProject.class,
        IFolder.class,
        IFile.class,
        IResource.class,
        IWorkbenchAdapter.class
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
        if (adapterType == DBPDataSourceContainer.class) {
            if (adaptableObject instanceof DBNDataSource) {
                return adapterType.cast(((DBNDataSource) adaptableObject).getDataSourceContainer());
            }
            DBSObject object = DBUtils.getFromObject(adaptableObject);
            if (object == null) {
                return null;
            }
            if (object instanceof DBPDataSourceContainer) {
                return adapterType.cast(object);
            }
            DBPDataSource dataSource = object.getDataSource();
            return dataSource == null ? null : adapterType.cast(dataSource.getContainer());
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof DBNDatabaseFolder) {
                adaptableObject = ((DBNDatabaseFolder) adaptableObject).getParentObject();
            }
            DBPObject object = null;
            if (adaptableObject instanceof DBSWrapper) {
                object = ((DBSWrapper) adaptableObject).getObject();
            } else if (adaptableObject instanceof DBPObject) {
                object = (DBPObject) adaptableObject;
            }
            if (object instanceof DBSObject) {
                object = DBUtils.getPublicObject((DBSObject) object);
            }
            if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                return adapterType.cast(object);
            }
//        } else if (IProject.class == adapterType) {
//            DBPProject project = null;
//            if (adaptableObject instanceof DBNNode) {
//                project = ((DBNNode) adaptableObject).getOwnerProject();
//            }
//            return project == null ? null : adapterType.cast(project.getEclipseProject());
        } else if (IResource.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof DBNResource) {
                return ((DBNResource) adaptableObject).getAdapter(adapterType);
            } else if (adaptableObject instanceof DBNNode node) {
                final IResource resource = node.getAdapter(IResource.class);
                if (adapterType.isInstance(resource)) {
                    return adapterType.cast(resource);
                }
            }
        } else if (adapterType == IPropertySource.class) {
            DBPObject dbObject = null;
            if (adaptableObject instanceof DBSWrapper wrapper) {
                dbObject = wrapper.getObject();
            } else if (adaptableObject instanceof DBPObject) {
                dbObject = (DBPObject) adaptableObject;
            }
            if (dbObject instanceof IPropertySource) {
                return adapterType.cast(dbObject);
            }
            if (dbObject instanceof DBPPropertySource) {
                return adapterType.cast(new PropertySourceDelegate((DBPPropertySource) dbObject));
            }
            if (dbObject instanceof IAdaptable adaptable) {
                Object adapter = adaptable.getAdapter(IPropertySource.class);
                if (adapter != null) {
                    return adapterType.cast(adapter);
                }
                adapter = adaptable.getAdapter(DBPPropertySource.class);
                if (adapter != null) {
                    return adapterType.cast(new PropertySourceDelegate((DBPPropertySource) adapter));
                }
            }
            if (dbObject != null) {
                PropertyCollector props = new PropertyCollector(adaptableObject, dbObject , true);
                props.collectProperties();
                if (props.isEmpty() && adaptableObject instanceof DBSObject meta) {
                    // Add default properties
                    props.addProperty(null, DBConstants.PROP_ID_NAME, ModelMessages.model_navigator_Name, meta.getName()); //$NON-NLS-1$
                    props.addProperty(null, "desc", ModelMessages.model_navigator_Description, meta.getDescription()); //$NON-NLS-1$
                }
                return adapterType.cast(new PropertySourceDelegate(props));
            }
        } else if (adapterType == IWorkbenchAdapter.class) {
            // Workbench adapter
            if (adaptableObject instanceof DBNNode node) {
                WorkbenchAdapter workbenchAdapter = new WorkbenchAdapter() {
                    @Override
                    public ImageDescriptor getImageDescriptor(Object object) {
                        return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
                    }

                    @Override
                    public String getLabel(Object o) {
                        return node.getNodeDisplayName();
                    }

                    @Override
                    public Object getParent(Object o) {
                        return node.getParentNode();
                    }
                };
                return adapterType.cast(workbenchAdapter);
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTER_LIST;
    }
}