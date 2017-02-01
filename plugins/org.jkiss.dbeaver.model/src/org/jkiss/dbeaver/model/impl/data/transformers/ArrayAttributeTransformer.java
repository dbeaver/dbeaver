/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;

import java.util.List;
import java.util.Map;

/**
 * Transforms attribute of array type into hierarchy of attributes
 */
public class ArrayAttributeTransformer implements DBDAttributeTransformer {

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        DBSDataType collectionType;
        if (attribute.getAttribute() instanceof DBSTypedObjectEx) {
            collectionType = ((DBSTypedObjectEx) attribute.getAttribute()).getDataType();
        } else {
            collectionType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), attribute.getTypeName());
        }
        if (collectionType != null) {
            DBSDataType componentType = collectionType.getComponentType(session.getProgressMonitor());
            if (componentType instanceof DBSEntity) {
                ComplexTypeAttributeTransformer.createNestedTypeBindings(session, attribute, rows, (DBSEntity) componentType);
                return;
            }
        }
        // No component type found.
        // Array items should be resolved in a lazy mode
        MapAttributeTransformer.resolveMapsFromData(session, attribute, rows);
    }

}
