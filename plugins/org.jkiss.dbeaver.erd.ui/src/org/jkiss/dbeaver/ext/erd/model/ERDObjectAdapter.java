/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gef.EditPart;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * ERD object adapter
 */
public class ERDObjectAdapter implements IAdapterFactory {

    public ERDObjectAdapter() {
    }

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (DBNNode.class == adapterType) {
            Object model = ((EditPart) adaptableObject).getModel();
            if (model instanceof ERDObject) {
                Object object = ((ERDObject<?>) model).getObject();
                if (object instanceof DBSObject) {
                    DBNDatabaseNode node = DBNUtils.getNodeByObject((DBSObject) object);
                    if (node != null) {
                        return adapterType.cast(node);
                    }
                }
            }
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof EditPart) {
                Object model = ((EditPart) adaptableObject).getModel();
                if (model != null && adapterType.isAssignableFrom(model.getClass())) {
                    return adapterType.cast(model);
                }
                if (model instanceof ERDObject) {
                    Object object = ((ERDObject<?>) model).getObject();
                    if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                        return adapterType.cast(object);
                    }
                }
            }
        } else if (DBPPropertySource.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof EditPart) {
                Object model = ((EditPart) adaptableObject).getModel();
                if (model instanceof ERDObject) {
                    return ((ERDObject<?>) model).getAdapter(adapterType);
                }
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { ERDObject.class, DBPNamedObject.class, DBPQualifiedObject.class, DBSObject.class, DBNNode.class };
    }
}
