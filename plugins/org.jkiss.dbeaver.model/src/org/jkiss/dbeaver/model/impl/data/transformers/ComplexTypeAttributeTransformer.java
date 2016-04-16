/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
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
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
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
