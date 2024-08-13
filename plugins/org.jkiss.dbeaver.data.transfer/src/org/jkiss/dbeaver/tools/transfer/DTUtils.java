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
package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskInfoCollector;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.serialize.DTObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.serialize.SerializerContext;
import org.jkiss.dbeaver.tools.transfer.serialize.SerializerRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Data transfer utils
 */
public class DTUtils {

    private static final Log log = Log.getLog(DTUtils.class);

    public static void addSummary(StringBuilder summary, String option, Object value) {
        summary.append("\t").append(option).append(": ").append(value).append("\n");
    }

    public static void addSummary(StringBuilder summary, String option, boolean value) {
        summary.append("\t").append(option).append(": ").append(value ? "Yes" : "No").append("\n");
    }

    public static void addSummary(StringBuilder summary, DataTransferProcessorDescriptor processor, Map<?, ?> props) {
        summary.append(NLS.bind(DTMessages.data_transfer_summary_title, processor.getName())).append(":\n");
        for (DBPPropertyDescriptor prop : processor.getProperties()) {
            Object propValue = props.get(prop.getId());
            if (propValue == null) {
                propValue = prop.getDefaultValue();
            }
            if (propValue != null) {
                addSummary(summary, prop.getDisplayName(), propValue);
            }
        }
    }

    public static String getTableName(DBPDataSource dataSource, DBPNamedObject source, boolean shortName) {
        if (source instanceof DBSEntity) {
            return shortName ?
                DBUtils.getQuotedIdentifier((DBSObject) source) :
                DBUtils.getObjectFullName(source, DBPEvaluationContext.UI);
        } else {
            String tableName = null;
            if (source instanceof SQLQueryContainer) {
                tableName = getTableNameFromQuery(dataSource, (SQLQueryContainer) source, shortName);
            } else if (source instanceof IAdaptable) {
                SQLQueryContainer queryContainer = ((IAdaptable) source).getAdapter(SQLQueryContainer.class);
                if (queryContainer != null) {
                    tableName = getTableNameFromQuery(dataSource, queryContainer, shortName);
                }
            }
            if (tableName == null && source instanceof IAdaptable) {
                DBSDataContainer dataContainer = ((IAdaptable) source).getAdapter(DBSDataContainer.class);
                if (dataContainer instanceof DBSEntity) {
                    tableName = shortName ?
                        DBUtils.getQuotedIdentifier(dataContainer) :
                        DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.UI);
                }
            }
            if (tableName == null) {
                return shortName ?
                    DBUtils.getQuotedIdentifier(dataSource, source.getName()) :
                    DBUtils.getObjectFullName(source, DBPEvaluationContext.DML);
            }
            return tableName;
        }
    }


    /**
     * Use this method for the export cases. E.g. if "table" used as a pattern
     * and you do not want to see all statement for query with JOINs etc. instead of clear table name.
     * Methods returns default value in this case.
     *
     * @param dataSource dataSource
     * @param queryContainer not nullable query container
     * @return table name founded in the query or {@code null}
     */
    @Nullable
    public static String getTableNameFromQueryContainer(DBPDataSource dataSource, @NotNull SQLQueryContainer queryContainer) {
        if (dataSource == null) {
            return null;
        }
        String nameFromQuery = DTUtils.getTableNameFromQuery(dataSource, queryContainer, true);
        if (CommonUtils.isEmpty(nameFromQuery)) {
            // Use default pattern name for this case, not the all statement
            return null;
        }
        return nameFromQuery;
    }

    public static String getTableNameFromQuery(DBPDataSource dataSource, SQLQueryContainer queryContainer, boolean shortName) {
        SQLScriptElement query = queryContainer.getQuery();
        if (query instanceof SQLQuery) {
            DBCEntityMetaData singleSource = ((SQLQuery) query).getEntityMetadata(true);
            if (singleSource != null) {
                SQLDialect dialect = dataSource.getSQLDialect();
                String entity = transformName(dialect, singleSource.getEntityName());
                if (shortName) {
                    return entity;
                }
                String schema = transformName(dialect, singleSource.getSchemaName());
                String catalog = transformName(dialect, singleSource.getCatalogName());
                String structSeparator = String.valueOf(dialect.getStructSeparator());
                StringBuilder nameBuilder = new StringBuilder();
                if (catalog != null) {
                    nameBuilder.append(catalog).append(structSeparator);
                }
                if (schema != null) {
                    nameBuilder.append(schema).append(structSeparator);
                }
                nameBuilder.append(entity);
                return nameBuilder.toString();
            }
        }
        return null;
    }

    /**
     * Return merged source entities names as one big target name for the export goals.
     *
     * @param queryContainer container which contains a query
     * @return string representation of entities names
     */
    @Nullable
    public static String getTargetContainersNameFromQuery(@NotNull SQLQueryContainer queryContainer) {
        SQLScriptElement query = queryContainer.getQuery();
        if (query instanceof SQLQuery) {
            List<String> selectEntitiesNames = ((SQLQuery) query).getAllSelectEntitiesNames();
            if (!CommonUtils.isEmpty(selectEntitiesNames)) {
                StringJoiner names = new StringJoiner("_");
                selectEntitiesNames.forEach(names::add);
                return names.toString();
            }
        }
        return null;
    }

    @Nullable
    private static String transformName(@NotNull SQLDialect dialect, @Nullable String name) {
        if (name == null) {
            return null;
        }
        if (dialect.isQuotedIdentifier(name)) {
            return name;
        }
        DBPIdentifierCase identifierCase = dialect.storesUnquotedCase();
        return identifierCase.transform(name);
    }

    public static void closeContents(@NotNull DBCResultSet resultSet, @NotNull DBDContent content) {
        if (resultSet.getFeature(DBCResultSet.FEATURE_NAME_LOCAL) != null) {
            return;
        }
        content.release();
    }

    public static <OBJECT_CONTEXT, OBJECT_TYPE> Object deserializeObject(
        @NotNull DBRRunnableContext runnableContext,
        SerializerContext serializeContext,
        OBJECT_CONTEXT objectContext,
        @NotNull Map<String, Object> objectConfig
    ) throws DBCException {
        String typeID = CommonUtils.toString(objectConfig.get("type"));
        DTObjectSerializer<OBJECT_CONTEXT, OBJECT_TYPE> serializer = SerializerRegistry.getInstance().createSerializerByType(typeID);
        if (serializer == null) {
            return null;
        }
        Map<String, Object> location = JSONUtils.getObject(objectConfig, "location");
        try {
            return serializer.deserializeObject(runnableContext, serializeContext, objectContext, location);
        } catch (DBException e) {
            throw new RuntimeException(e);
        }
    }

    public static <OBJECT_CONTEXT, OBJECT_TYPE> Map<String, Object> serializeObject(
        DBRRunnableContext runnableContext,
        OBJECT_CONTEXT context,
        @NotNull OBJECT_TYPE object
    )  throws DBException {
        DTObjectSerializer<OBJECT_CONTEXT, OBJECT_TYPE> serializer = SerializerRegistry.getInstance().createSerializer(object);
        if (serializer == null) {
            return null;
        }
        Map<String, Object> state = new LinkedHashMap<>();

        Map<String, Object> location = new LinkedHashMap<>();
        serializer.serializeObject(runnableContext, context, object, location);
        state.put("type", SerializerRegistry.getInstance().getObjectType(object));
        state.put("location", location);

        return state;
    }

    public static void collectTaskInfo(
        DBTTask task,
        Map<String, Object> objectConfig,
        DBTTaskInfoCollector.TaskInformation information
    ) {

    }

}
