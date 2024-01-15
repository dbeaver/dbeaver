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
package org.jkiss.dbeaver.ext.bigquery.model.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.bigquery.model.BigQueryConstants;
import org.jkiss.dbeaver.ext.bigquery.model.BigQueryDataSource;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeMap;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BigQueryStructValueHandler extends JDBCStructValueHandler {
    
    private static final Log log = Log.getLog(BigQueryStructValueHandler.class);
    private static final Gson gson = new GsonBuilder().create();

    private static class ColumnFieldInfo {
        private final String name;
        private final String type;
        private final String mode;
        private ColumnFieldInfo[] fields;

        public ColumnFieldInfo(String name, String type, String mode) {
            this.name = name;
            this.type = type;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return name + "; type=" + type + "; mode=" + mode + "; fields=(" + Arrays.toString(fields) + ")";
        }
    }

    private DBSDataType structDataType;
    private DBSDataType arrayDataType;
    private ColumnFieldInfo[] columnFields;

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        try {
            BigQueryDataSource dataSource = (BigQueryDataSource) session.getDataSource();
            if (dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES) && structDataType == null) {
                structDataType = dataSource.getLocalDataType(BigQueryConstants.DATA_TYPE_STRUCT);
                arrayDataType = dataSource.getLocalDataType(BigQueryConstants.DATA_TYPE_ARRAY);

                ResultSet bqResultSet = resultSet.getOriginal();
                if (bqResultSet.getClass().getName().startsWith("com.simba.googlebigquery.jdbc")) {
                    List<?> rsColumnsMeta = (List<?>) BeanUtils.invokeObjectDeclaredMethod(
                        bqResultSet, "getResultSetColumns", new Class[0], new Object[0]);
                    Object columnMetaData = rsColumnsMeta.get(index - 1);  // BQColumnMetadata

                    List<?> nestedTypes = (List<?>) BeanUtils.invokeObjectDeclaredMethod(
                        columnMetaData, "getNestedTypes", new Class[0], new Object[0]);
                    if (nestedTypes != null) {
                        columnFields = collectNestedTypes(nestedTypes);
                    }
                }
            }
        } catch (Throwable e) {
            log.debug("Error reading BQ struct metadata", e);
        }
        return super.fetchColumnValue(session, resultSet, type, index);
    }

    private ColumnFieldInfo[] collectNestedTypes(List<?> nestedTypes) throws Exception {
        ColumnFieldInfo[] fields = new ColumnFieldInfo[nestedTypes.size()];
        for (int i = 0; i < nestedTypes.size(); i++) {
            Object meta = nestedTypes.get(i);
            fields[i] = new ColumnFieldInfo(
                (String) BeanUtils.readObjectProperty(meta, "name"),
                (String) BeanUtils.readObjectProperty(meta, "type"),
                (String) BeanUtils.readObjectProperty(meta, "mode")
            );
            Object fieldsObj = BeanUtils.readObjectProperty(meta, "fields");
            if (fieldsObj instanceof List<?> list) {
                fields[i].fields = collectNestedTypes(list);
            }
        }
        return fields;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object instanceof DBDValue) {
            return object;
        }

        final String typeName = type.getTypeName();

        if (object == null) {
            return null;
        }
        if (structDataType != null) {
            if (object instanceof String strValue) {
                try {
                    Map<String, Object> map = JSONUtils.parseMap(gson, new StringReader(strValue));

                    return convertStructToValue(session, columnFields, map);
                } catch (Exception e) {
                    throw new DBCException("Can't parse json: " + typeName, e);
                }
            } else {
                return super.getValueFromObject(session, type, object, copy, validateValue);
            }
        }
        return object;
    }

    private Object convertStructToValue(DBCSession session, ColumnFieldInfo[] columnFields, Map<String, Object> struct) throws DBCException {
        Map<String, Object> result = new LinkedHashMap<>();
        Object value = struct.get("v");
        if (value == null) {
            value = struct;
        }
        List<Map<String, Object>> fieldValues = null;
        if (value instanceof Map vm) {
            fieldValues = JSONUtils.getObjectList(vm, "f");
        } else {
            return transformValue(session, value, null);
        }

        if (!CommonUtils.isEmpty(fieldValues)) {
            for (int i = 0; i < fieldValues.size(); i++) {
                Map<String, Object> field = fieldValues.get(i);
                Object fieldValue = field.get("v");
                if (fieldValue == null) {
                    log.debug("Field value missing");
                    continue;
                }
                if (columnFields == null) {
                    result.put("1", fieldValue);
                } else if (i < columnFields.length) {
                    ColumnFieldInfo columnField = columnFields[i];
                    fieldValue = transformValue(session, fieldValue, columnField);
                    result.put(columnField.name, fieldValue);
                } else {
                    log.debug("Field value out of index (" + i + ">=" + columnFields.length);
                }

            }
        }
        return new JDBCCompositeMap(session, structDataType, result);
    }

    private Object transformValue(DBCSession session, Object fieldValue, ColumnFieldInfo columnField) throws DBCException {
        if (fieldValue instanceof List<?> listValue) {
            List<Object> result = new ArrayList<>(listValue.size());
            for (Object item : listValue) {
                result.add(transformValue(session, item, columnField));
            }
            fieldValue = new JDBCCollection(session.getProgressMonitor(), arrayDataType, this, result.toArray());
        } else if (fieldValue instanceof Map mapValue) {
            if (columnField != null && !ArrayUtils.isEmpty(columnField.fields)) {
                fieldValue = convertStructToValue(session, columnField.fields, mapValue);
            } else {
                Object nestedValue = mapValue.get("v");
                if (nestedValue != null) {
                    fieldValue = nestedValue;
                }
            }
        }
        return fieldValue;
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value instanceof JDBCComposite) {
            Object[] values = ((JDBCComposite) value).getValues();
            if (!ArrayUtils.isEmpty(values)) {
                return DBValueFormatting.getDefaultValueDisplayString(values, format);
            }
        }
        return super.getValueDisplayString(column, value, format);
    }
}
