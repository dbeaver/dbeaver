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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingType;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transforms attribute of complex type into hierarchy of attributes
 */
public class ComplexTypeAttributeTransformer implements DBDAttributeTransformer {

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        if (!session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES)) {
            return;
        }
        DBSDataType dataType;
        if (attribute.getAttribute() instanceof DBSTypedObjectEx) {
            dataType = ((DBSTypedObjectEx) attribute.getAttribute()).getDataType();
        } else {
            dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), attribute.getTypeName());
        }
        if (dataType instanceof DBSEntity) {
            createNestedTypeBindings(session, attribute, rows, (DBSEntity) dataType);
        }
    }

    static void createNestedTypeBindings(DBCSession session, DBDAttributeBinding attribute, List<Object[]> rows, DBSEntity dataType) throws DBException {
        List<DBDAttributeBinding> nestedBindings = new ArrayList<>();
        for (DBSEntityAttribute nestedAttr : CommonUtils.safeCollection(dataType.getAttributes(session.getProgressMonitor()))) {
            DBDAttributeBindingType nestedBinding = new DBDAttributeBindingType(attribute, nestedAttr);
            nestedBinding.lateBinding(session, rows);
            nestedBindings.add(nestedBinding);
        }
        if (!nestedBindings.isEmpty()) {
            attribute.setNestedBindings(nestedBindings);
        }
    }

}
