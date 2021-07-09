/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract node
 */
public class DTUtils {

    public static void addSummary(StringBuilder summary, String option, Object value) {
        summary.append("\t").append(option).append(": ").append(value).append("\n");
    }

    public static void addSummary(StringBuilder summary, String option, boolean value) {
        summary.append("\t").append(option).append(": ").append(value ? "Yes" : "No").append("\n");
    }

    public static void addSummary(StringBuilder summary, DataTransferProcessorDescriptor processor, Map<?, ?> props) {
        summary.append(processor.getName()).append(" settings:\n");
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

    public static String getTableNameFromQuery(DBPDataSource dataSource, SQLQueryContainer queryContainer, boolean shortName) {
        SQLScriptElement query = queryContainer.getQuery();
        if (query instanceof SQLQuery) {
            DBCEntityMetaData singleSource = ((SQLQuery) query).getSingleSource();
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

    @NotNull
    public static List<DBSAttributeBase> getAttributes(@NotNull DBRProgressMonitor monitor, @NotNull DBSDataContainer container, @NotNull Object controller) throws DBException {
        final List<DBSAttributeBase> attributes = new ArrayList<>();
        if (container instanceof DBSEntity && !(container instanceof DBSDocumentContainer)) {
            for (DBSEntityAttribute attr : CommonUtils.safeList(((DBSEntity) container).getAttributes(monitor))) {
                if (DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                attributes.add(attr);
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
                    container.readData(new AbstractExecutionSource(container, session.getExecutionContext(), controller), session, receiver, null, 0, 1, DBSDataContainer.FLAG_NONE, 1);
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
                    attributes.add(attr);
                }
            });
        }

        return attributes;
    }

    private static class MetadataReceiver implements DBDDataReceiver {
        private final DBSDataContainer container;
        private DBDAttributeBinding[] attributes;

        public MetadataReceiver(DBSDataContainer container) {
            this.container = container;
        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
            attributes = DBUtils.makeLeafAttributeBindings(session, container, resultSet);
        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {
        }

        @Override
        public void close() {
        }
    }
}
