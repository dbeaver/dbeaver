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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Virtual model utils
 */
public abstract class DBVUtils {

    @Nullable
    public static DBVTransformSettings getTransformSettings(@NotNull DBDAttributeBinding binding, boolean create) {
        DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
        if (entityAttribute != null) {
            DBVEntity vEntity = findVirtualEntity(entityAttribute.getParentObject(), create);
            if (vEntity != null) {
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, create);
                if (vAttr != null) {
                    return getTransformSettings(vAttr, create);
                }
            }
        }
        return null;
    }

    @Nullable
    private static DBVTransformSettings getTransformSettings(@NotNull DBVEntityAttribute attribute, boolean create) {
        if (attribute.getTransformSettings() != null) {
            return attribute.getTransformSettings();
        } else if (create) {
            attribute.setTransformSettings(new DBVTransformSettings());
            return attribute.getTransformSettings();
        }
        for (DBVObject object = attribute.getParentObject(); object != null; object = object.getParentObject()) {
            if (object.getTransformSettings() != null) {
                return object.getTransformSettings();
            }
        }
        return null;
    }

    @NotNull
    public static Map<String, String> getAttributeTransformersOptions(@NotNull DBDAttributeBinding binding) {
        Map<String, String> options = null;
        final DBVTransformSettings transformSettings = getTransformSettings(binding, false);
        if (transformSettings != null) {
            options = transformSettings.getTransformOptions();
        }
        if (options != null) {
            return options;
        }
        return Collections.emptyMap();
    }

    @Nullable
    public static DBVEntity findVirtualEntity(@NotNull DBSEntity source, boolean create)
    {
        return source.getDataSource().getContainer().getVirtualModel().findEntity(source, create);
    }

    @Nullable
    public static DBDAttributeTransformer[] findAttributeTransformers(@NotNull DBDAttributeBinding binding, @Nullable Boolean custom)
    {
        DBPDataSource dataSource = binding.getDataSource();
        DBPDataSourceContainer container = dataSource.getContainer();
        List<? extends DBDAttributeTransformerDescriptor> tdList =
            container.getPlatform().getValueHandlerRegistry().findTransformers(dataSource, binding.getAttribute(), custom);
        if (tdList == null || tdList.isEmpty()) {
            return null;
        }
        boolean filtered = false;
        DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
        if (entityAttribute != null) {
            DBVEntity vEntity = findVirtualEntity(entityAttribute.getParentObject(), false);
            if (vEntity != null) {
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, false);
                if (vAttr != null) {
                    final DBVTransformSettings transformSettings = getTransformSettings(vAttr, false);
                    if (transformSettings != null) {
                        filtered = transformSettings.filterTransformers(tdList);
                    }
                }
            }
        }

        if (!filtered) {
            // Leave only default transformers
            for (int i = 0; i < tdList.size();) {
                if (tdList.get(i).isCustom() || !tdList.get(i).isApplicableByDefault()) {
                    tdList.remove(i);
                } else {
                    i++;
                }
            }
        }
        if (tdList.isEmpty()) {
            return null;
        }
        DBDAttributeTransformer[] result = new DBDAttributeTransformer[tdList.size()];
        for (int i = 0; i < tdList.size(); i++) {
            result[i] = tdList.get(i).getInstance();
        }
        return result;
    }

    public static String getDictionaryDescriptionColumns(DBRProgressMonitor monitor, DBSEntityAttribute attribute) throws DBException {
        DBVEntity dictionary = DBVUtils.findVirtualEntity(attribute.getParentObject(), false);
        String descColumns = null;
        if (dictionary != null) {
            descColumns = dictionary.getDescriptionColumnNames();
        }
        if (descColumns == null) {
            descColumns = DBVEntity.getDefaultDescriptionColumn(monitor, attribute);
        }
        return descColumns;
    }

    @NotNull
    public static List<DBDLabelValuePair> readDictionaryRows(
        DBCSession session,
        DBSEntityAttribute valueAttribute,
        DBDValueHandler valueHandler,
        DBCResultSet dbResult) throws DBCException
    {
        List<DBDLabelValuePair> values = new ArrayList<>();
        List<DBCAttributeMetaData> metaColumns = dbResult.getMeta().getAttributes();
        List<DBDValueHandler> colHandlers = new ArrayList<>(metaColumns.size());
        for (DBCAttributeMetaData col : metaColumns) {
            colHandlers.add(DBUtils.findValueHandler(session, col));
        }
        boolean hasNulls = false;
        // Extract enumeration values and (optionally) their descriptions
        while (dbResult.nextRow()) {
            // Check monitor
            if (session.getProgressMonitor().isCanceled()) {
                break;
            }
            // Get value and description
            Object keyValue = valueHandler.fetchValueObject(session, dbResult, valueAttribute, 0);
            if (DBUtils.isNullValue(keyValue)) {
                if (hasNulls) {
                    continue;
                }
                hasNulls = true;
            }
            String keyLabel;
            if (metaColumns.size() > 1) {
                StringBuilder keyLabel2 = new StringBuilder();
                for (int i = 1; i < colHandlers.size(); i++) {
                    Object descValue = colHandlers.get(i).fetchValueObject(session, dbResult, metaColumns.get(i), i);
                    if (keyLabel2.length() > 0) {
                        keyLabel2.append(" ");
                    }
                    keyLabel2.append(colHandlers.get(i).getValueDisplayString(metaColumns.get(i), descValue, DBDDisplayFormat.NATIVE));
                }
                keyLabel = keyLabel2.toString();
            } else {
                keyLabel = valueHandler.getValueDisplayString(valueAttribute, keyValue, DBDDisplayFormat.NATIVE);
            }
            values.add(new DBDLabelValuePair(keyLabel, keyValue));
        }
        return values;
    }

}
