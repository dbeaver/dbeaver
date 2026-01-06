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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Transforms attribute of map type into hierarchy of attributes
 */
public class MapAttributeTransformer implements DBDAttributeTransformer {

    private static final boolean FILTER_SIMPLE_COLLECTIONS = false;

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        if (!session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES)) {
            return;
        }
        if (attribute.getDataKind() == DBPDataKind.STRUCT &&
            !CommonUtils.isEmpty(attribute.getNestedBindings()) &&
            !session.getDataSource().getInfo().isDynamicMetadata()
        ) {
            // Do not transform structs to avoid double transformation
            return;
        }
        if (rows.isEmpty()) {
            // Make a fake row with empty document in it
            int attrIndex = attribute.getOrdinalPosition();
            Object[] fakeRow = new Object[attrIndex + 1];
            fakeRow[attrIndex] = attribute.getValueHandler().createNewValueObject(session, attribute);
            resolveMapsFromData(session, attribute, Collections.singletonList(fakeRow));
        } else {
            resolveMapsFromData(session, attribute, rows);
        }
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
                } else {
                    // Skip empty collections - we can't get any info out of them
                    continue;
                }
            }

            if (value instanceof DBDComposite composite) {
                // Fill value attributes for all rows
                DBSAttributeBase[] attributes = composite.getAttributes();
                for (DBSAttributeBase attr : attributes) {
                    Pair<DBSAttributeBase, Object[]> attrValue = findAttributeValue(attr, valueAttributes);
                    if (attrValue != null) {
                        // Update attr value
                        attrValue.getSecond()[i] = composite.getAttributeValue(attr);
                    } else {
                        Object[] valueList = new Object[rows.size()];
                        valueList[i] = composite.getAttributeValue(attr);
                        if (valueAttributes == null) {
                            valueAttributes = new ArrayList<>();
                        }
                        Pair<DBSAttributeBase, Object[]> attributePair = new Pair<>(
                            attr,
                            valueList);
                        if (valueAttributes.size() >= attr.getOrdinalPosition()) {
                            valueAttributes.add(attr.getOrdinalPosition(), attributePair);
                        } else {
                            valueAttributes.add(attributePair);
                        }
                    }
                }
            }
        }
        if (valueAttributes != null && !valueAttributes.isEmpty()) {
            createNestedMapBindings(session, attribute, valueAttributes, rows);
        }
    }

    private static Pair<DBSAttributeBase, Object[]> findAttributeValue(
        @NotNull DBSAttributeBase attr,
        @Nullable List<Pair<DBSAttributeBase, Object[]>> valueAttributes)
    {
        if (valueAttributes == null) {
            return null;
        }
        for (Pair<DBSAttributeBase, Object[]> pair : valueAttributes) {
            if (pair.getFirst().getName().equals(attr.getName())) {
                pair.setFirst(DBUtils.getMoreCommonType(pair.getFirst(), attr));
                return pair;
            }
        }
        return null;
    }

    private static void createNestedMapBindings(
        DBCSession session,
        DBDAttributeBinding topAttribute,
        List<Pair<DBSAttributeBase, Object[]>> nestedAttributes,
        List<Object[]> rows) throws DBException
    {
        int maxPosition = 0;
        List<DBDAttributeBinding> nestedBindings = topAttribute.getNestedBindings();
        if (nestedBindings == null) {
            nestedBindings = new ArrayList<>();
        }
        for (Pair<DBSAttributeBase, Object[]> nestedAttr : nestedAttributes) {
            DBSAttributeBase attribute = nestedAttr.getFirst();
            maxPosition = Math.max(maxPosition, attribute.getOrdinalPosition());
            DBDAttributeBinding nestedBinding = DBUtils.findObject(nestedBindings, attribute.getName());
            if (nestedBinding == null) {
                nestedBinding = new DBDAttributeBindingType(topAttribute, attribute, nestedBindings.size());
                nestedBindings.add(nestedBinding);
            }
            maxPosition = Math.max(maxPosition, nestedBinding.getOrdinalPosition());
        }


        Object[] fakeRow = new Object[maxPosition + 1];

        List<Object[]> fakeRows = Collections.singletonList(fakeRow);
        for (Pair<DBSAttributeBase, Object[]> nestedAttr : nestedAttributes) {
            DBSAttributeBase attribute = nestedAttr.getFirst();
            Object[] values = nestedAttr.getSecond();
            DBDAttributeBinding nestedBinding = DBUtils.findObject(nestedBindings, attribute.getName());
            if (nestedBinding == null) {
                nestedBinding = new DBDAttributeBindingType(topAttribute, attribute, nestedBindings.size());
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
            } else {
                nestedBinding.lateBinding(session, fakeRows);
            }
        }

        if (FILTER_SIMPLE_COLLECTIONS) {
            // Remove empty collection attributes from nested bindings
            // They can't be used anyway
            nestedBindings.removeIf(
                attribute -> {
                    if (attribute.getDataKind() == DBPDataKind.ARRAY && CommonUtils.isEmpty(attribute.getNestedBindings())) {
                        return true;
                    }
                    return false;
                });
        }

        if (!nestedBindings.isEmpty()) {
            topAttribute.setNestedBindings(nestedBindings);
        }
    }

}
