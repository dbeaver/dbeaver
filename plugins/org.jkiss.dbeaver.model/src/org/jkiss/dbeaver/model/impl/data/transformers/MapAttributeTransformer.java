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
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Transforms attribute of map type into hierarchy of attributes
 */
public class MapAttributeTransformer implements DBDAttributeTransformer {

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        resolveMapsFromData(session, attribute, rows);
    }

    static void resolveMapsFromData(DBCSession session, DBDAttributeBinding attribute, List<Object[]> rows) throws DBException {

        // Analyse rows and extract meta information from values
        List<Pair<DBSAttributeBase, Object[]>> valueAttributes = null;
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i)[attribute.getOrdinalPosition()];
            if (value instanceof DBDCollection) {
                // Use first element to discover structure
                DBDCollection collection = (DBDCollection) value;
                if (collection.getItemCount() > 0) {
                    value = collection.getItem(0);
                }
            }

            if (value instanceof DBDComposite) {
                DBSAttributeBase[] attributes = ((DBDComposite) value).getAttributes();
                for (DBSAttributeBase attr : attributes) {
                    Pair<DBSAttributeBase, Object[]> attrValue = null;
                    if (valueAttributes != null) {
                        for (Pair<DBSAttributeBase, Object[]> pair : valueAttributes) {
                            if (pair.getFirst().getName().equals(attr.getName())) {
                                attrValue = pair;
                                break;
                            }
                        }
                    }
                    if (attrValue != null) {
                        // Update attr value
                        attrValue.getSecond()[i] = ((DBDComposite) value).getAttributeValue(attr);
                    } else {
                        Object[] valueList = new Object[rows.size()];
                        valueList[i] = ((DBDComposite) value).getAttributeValue(attr);
                        if (valueAttributes == null) {
                            valueAttributes = new ArrayList<>();
                        }
                        valueAttributes.add(
                            new Pair<>(
                                attr,
                                valueList));
                    }
                }
            }
        }
        if (valueAttributes != null && !valueAttributes.isEmpty()) {
            createNestedMapBindings(session, attribute, valueAttributes);
        }
    }

    private static void createNestedMapBindings(DBCSession session, DBDAttributeBinding topAttribute, List<Pair<DBSAttributeBase, Object[]>> nestedAttributes) throws DBException {
        int maxPosition = 0;
        for (Pair<DBSAttributeBase, Object[]> attr : nestedAttributes) {
            maxPosition = Math.max(maxPosition, attr.getFirst().getOrdinalPosition());
        }
        List<DBDAttributeBinding> nestedBindings = topAttribute.getNestedBindings();
        if (nestedBindings == null) {
            nestedBindings = new ArrayList<>();
        } else {
            for (DBDAttributeBinding binding : nestedBindings) {
                maxPosition = Math.max(maxPosition, binding.getOrdinalPosition());
            }
        }
        Object[] fakeRow = new Object[maxPosition + 1];

        List<Object[]> fakeRows = Collections.singletonList(fakeRow);
        for (Pair<DBSAttributeBase, Object[]> nestedAttr : nestedAttributes) {
            DBSAttributeBase attribute = nestedAttr.getFirst();
            Object[] values = nestedAttr.getSecond();
            DBDAttributeBinding nestedBinding = null;
            for (DBDAttributeBinding binding : nestedBindings) {
                if (binding.getName().equals(attribute.getName())) {
                    nestedBinding = binding;
                    break;
                }
            }
            if (nestedBinding == null) {
                nestedBinding = new DBDAttributeBindingType(topAttribute, attribute);
                nestedBindings.add(nestedBinding);
            }
            if (attribute.getDataKind().isComplex()) {
                // Make late binding for each row value
                for (int i = 0; i < values.length; i++) {
                    if (DBUtils.isNullValue(values[i])) {
                        continue;
                    }
                    fakeRow[nestedBinding.getOrdinalPosition()] = values[i];
                    nestedBinding.lateBinding(session, fakeRows);
                }
            }
        }

        if (!nestedBindings.isEmpty()) {
            topAttribute.setNestedBindings(nestedBindings);
        }
    }

}
