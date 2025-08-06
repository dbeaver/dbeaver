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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskInfoCollector;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.serialize.DTObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.serialize.SerializerContext;
import org.jkiss.dbeaver.tools.transfer.serialize.SerializerRegistry;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Data transfer utils
 */
public class DTUtils {

    private static final Log log = Log.getLog(DTUtils.class);
    private static final int MAX_SAMPLE_ROWS = 1000;

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

    /**
     * Retrieves the name of a table or equivalent object with a default fallback.
     *
     * @param dataSource   the data source associated with the object.
     * @param source       the object to retrieve the table name from (e.g., {@link DBSEntity}, {@link SQLQueryContainer}).
     * @param useShortName whether to return the short (quoted) name or the full name of the table.
     * @return the table name, or a default value if it cannot be resolved.
     */
    @NotNull
    public static String getTableName(
        @NotNull DBPDataSource dataSource,
        @NotNull DBPNamedObject source,
        boolean useShortName
    ) {
        return getTableName(dataSource, source, useShortName,
            useShortName ?
                DBUtils.getQuotedIdentifier(dataSource, source.getName()) :
                DBUtils.getObjectFullName(source, DBPEvaluationContext.DML));
    }

    /**
     * Retrieves the name of a table or equivalent object based on the provided source.
     *
     * @param dataSource   the data source associated with the object.
     * @param source       the object to retrieve the table name from (e.g., {@link DBSEntity}, {@link SQLQueryContainer}).
     * @param useShortName whether to return the short (quoted) name or the full name of the table.
     * @param defaultValue the value to return if the table name cannot be determined.
     * @return the table name, or {@code defaultValue} if it cannot be resolved.
     */
    @NotNull
    public static String getTableName(
        @NotNull DBPDataSource dataSource,
        @NotNull DBPNamedObject source,
        boolean useShortName,
        @NotNull String defaultValue
    ) {
        if (source instanceof DBSEntity) {
            return useShortName ?
                DBUtils.getQuotedIdentifier((DBSObject) source) :
                DBUtils.getObjectFullName(source, DBPEvaluationContext.UI);
        } else {
            String tableName = null;
            if (source instanceof SQLQueryContainer queryContainer) {
                tableName = getTableNameFromQuery(dataSource, queryContainer, useShortName);
            } else if (source instanceof IAdaptable adaptable) {
                SQLQueryContainer queryContainer = adaptable.getAdapter(SQLQueryContainer.class);
                if (queryContainer != null) {
                    tableName = getTableNameFromQuery(dataSource, queryContainer, useShortName);
                }
            }
            if (tableName == null && source instanceof IAdaptable adaptable) {
                DBSDataContainer dataContainer = adaptable.getAdapter(DBSDataContainer.class);
                if (dataContainer instanceof DBSEntity) {
                    tableName = useShortName ?
                        DBUtils.getQuotedIdentifier(dataContainer) :
                        DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.UI);
                }
            }

            return tableName == null ? defaultValue : tableName;
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

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends DBSAttributeBase & DBSObject> List<T> getAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataContainer container,
        @NotNull Object controller
    ) throws DBException {
        final List<T> attributes = new ArrayList<>();
        if (container instanceof DBSEntity && !(container instanceof DBSDocumentContainer)) {
            for (DBSEntityAttribute attr : CommonUtils.safeList(((DBSEntity) container).getAttributes(monitor))) {
                if (DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                attributes.add((T) attr);
            }
        } else {
            // Seems to be a dynamic query. Execute it to get metadata
            final DBCExecutionContext context = container instanceof DBPContextProvider
                ? ((DBPContextProvider) container).getExecutionContext()
                : DBUtils.getDefaultContext(container, false);
            if (context == null) {
                throw new DBCException("No execution context");
            }
            DBExecUtils.tryExecuteRecover(monitor, context.getDataSource(), monitor1 -> {
                final MetadataReceiver receiver = new MetadataReceiver(container);
                try (DBCSession session = context.openSession(monitor1, DBCExecutionPurpose.META, "Read query meta data")) {
                    AbstractExecutionSource executionSource = new AbstractExecutionSource(
                        container,
                        session.getExecutionContext(),
                        controller
                    );
                    container.readData(executionSource, session, receiver, null, 0, 1, DBSDataContainer.FLAG_NONE, 1);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
                if (receiver.attributes == null) {
                    throw new InvocationTargetException(new DBCException("Query does not contain any attributes"));
                }
                for (DBDAttributeBinding attr : receiver.attributes) {
                    if (DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    attributes.add((T) attr);
                }
            });
        }

        return attributes;
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

    /**
     * Returns "bottom" level attributes out of resultset.
     * For regular resultsets it is the same as getAttributeBindings, for complex types it returns only leaf attributes.
     */
    @NotNull
    public static DBDAttributeBinding[] makeLeafAttributeBindings(@NotNull DBCSession session, @NotNull DBSDataContainer dataContainer, @NotNull DBCResultSet resultSet) throws DBCException {
        List<DBDAttributeBinding> metaColumns = new ArrayList<>();
        List<? extends DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
        boolean isDocumentAttribute = attributes.size() == 1 && attributes.get(0).getDataKind() == DBPDataKind.DOCUMENT;
        if (isDocumentAttribute) {
            bindDocumentAttribute(session, dataContainer, resultSet, attributes, metaColumns);
        }
        if (metaColumns.isEmpty()) {
            for (DBCAttributeMetaData attribute : attributes) {
                DBDAttributeBindingMeta columnBinding = DBUtils.getAttributeBinding(dataContainer, session, attribute);
                metaColumns.add(columnBinding);
            }
        }

        List<DBDAttributeBinding> result = new ArrayList<>(metaColumns.size());
        for (DBDAttributeBinding binding : metaColumns) {
            addLeafBindings(result, binding);
        }

        if (!isDocumentAttribute) {
            // For documents we do binding earlier
            try {
                DBExecUtils.bindAttributes(
                    session,
                    dataContainer instanceof DBSEntity entity ? entity : null,
                    resultSet,
                    metaColumns.toArray(new DBDAttributeBinding[0]), null);
            } catch (Exception e) {
                log.debug("Error binding attributes", e);
            }
        }

        return DBUtils.injectAndFilterAttributeBindings(
            session.getDataSource(),
            dataContainer,
            result.toArray(new DBDAttributeBinding[0]),
            true);
    }

    private static void bindDocumentAttribute(
        @NotNull DBCSession session,
        @NotNull DBSDataContainer dataContainer,
        @NotNull DBCResultSet resultSet,
        List<? extends DBCAttributeMetaData> attributes,
        List<DBDAttributeBinding> metaColumns
    ) {
        DBCAttributeMetaData attributeMeta = attributes.get(0);
        DBDAttributeBindingMeta docBinding = DBUtils.getAttributeBinding(dataContainer, session, attributeMeta);
        try {
            List<Object[]> sampleRows = Collections.emptyList();
            if (resultSet instanceof DBCResultSetSampleProvider rssp) {
                session.getProgressMonitor().subTask("Read sample rows");
                sampleRows = rssp.getSampleRows(session, MAX_SAMPLE_ROWS);
            }
            session.getProgressMonitor().subTask("Discover attribute structure");
            docBinding.lateBinding(session, sampleRows);
        } catch (Exception e) {
            log.error("Document attribute '" + docBinding.getName() + "' binding error", e);
        }
        List<DBDAttributeBinding> nested = docBinding.getNestedBindings();
        if (!CommonUtils.isEmpty(nested)) {
            metaColumns.addAll(nested);
        } else {
            // No nested bindings. Try to get entity attributes
            try {
                DBSEntity docEntity = DBUtils.getEntityFromMetaData(session.getProgressMonitor(), session.getExecutionContext(), attributeMeta.getEntityMetaData());
                if (docEntity != null) {
                    Collection<? extends DBSEntityAttribute> entityAttrs = docEntity.getAttributes(session.getProgressMonitor());
                    if (!CommonUtils.isEmpty(entityAttrs)) {
                        for (DBSEntityAttribute ea : entityAttrs) {
                            metaColumns.add(new DBDAttributeBindingType(docBinding, ea, metaColumns.size()));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error getting attributes from document entity", e);
            }
        }
    }

    private static void addLeafBindings(List<DBDAttributeBinding> result, DBDAttributeBinding binding) {
        List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
        if (CommonUtils.isEmpty(nestedBindings)) {
            result.add(binding);
        } else {
            for (DBDAttributeBinding nested : nestedBindings) {
                addLeafBindings(result, nested);
            }
        }
    }

    public static Path findProjectFile(@NotNull DBPProject project, @NotNull String filePath) {
        Path file = project.getAbsolutePath().resolve(filePath);
        if (Files.exists(file) && Files.isRegularFile(file)) {
            return file;
        }
        return null;
    }

    private static class MetadataReceiver implements DBDDataReceiver {
        private final DBSDataContainer container;
        private DBDAttributeBinding[] attributes;

        public MetadataReceiver(DBSDataContainer container) {
            this.container = container;
        }

        @Override
        public void fetchStart(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
            attributes = makeLeafAttributeBindings(session, container, resultSet);
        }

        @Override
        public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) {
        }

        @Override
        public void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) {
        }

        @Override
        public void close() {
        }
    }
}
