/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreValueParser;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataTypeAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchemaMyTemp;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTypeType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeDynamic;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeStatic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCTransactionSavepoint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryType;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.WeakHashMap;
import javax.sql.rowset.RowSetMetaDataImpl;

/**
 * PostgreStructValueHandler
 */
public class PostgreStructValueHandler extends JDBCStructValueHandler {
    private static final Log log = Log.getLog(PostgreStructValueHandler.class);
    public static final PostgreStructValueHandler INSTANCE = new PostgreStructValueHandler();
    private static final String RECORD_QUERY_ALIAS = "dbeaver_record_query";
    private final Set<DBSTypedObject> unresolvedColumns = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private final Map<JDBCColumnMetaData, String[]> dynamicAttributeNames = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<JDBCColumnMetaData, RowSetMetaDataImpl> recordAttributeMetadata = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    protected void bindParameter(
        @NotNull JDBCSession session,
        @NotNull JDBCPreparedStatement statement,
        @NotNull DBSTypedObject paramType,
        int paramIndex,
        @Nullable Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, Types.STRUCT);
        } else if (value instanceof DBDComposite struct) {
            if (struct.isNull()) {
                statement.setNull(paramIndex, Types.STRUCT);
            } else if (struct instanceof JDBCComposite) {
                final Object[] values = ((JDBCComposite) struct).getValues();
                final String string = PostgreValueParser.generateObjectString(values);
                statement.setObject(paramIndex, string, Types.OTHER);
            }
        } else {
            throw new DBCException("Struct parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (PostgreConstants.TYPE_RECORD.equals(type.getTypeName())) {
            // Anonymous records have no registered attributes to resolve through pg_type.
            return convertToDynamicStruct(session, type, object, copy);
        }
        if (type instanceof JDBCColumnMetaData && unresolvedColumns.contains(type)) {
            return convertToDynamicStruct(session, type, object, copy);
        }
        PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();
        PostgreDataType structType = PostgreUtils.findDataType(session, dataSource, type);
        if (structType == null) {
            if (type instanceof JDBCColumnMetaData) {
                // A temporary row type is invisible to the metadata connection. Retry for new result sets,
                // but do not repeat the failed lookup for every row of this result set.
                unresolvedColumns.add(type);
            }
            if (!(type instanceof JDBCColumnMetaData)) {
                log.debug("Can't resolve struct type '" + type.getTypeName() + "'");
            }
            return type instanceof JDBCColumnMetaData || PostgreConstants.TYPE_RECORD.equals(type.getTypeName())
                ? convertToDynamicStruct(session, type, object, copy) : object;
        }
        if (structType.getTypeType() == PostgreTypeType.d) {
            // Domains are just wrappers around underlying type.
            structType = structType.getBaseType(session.getProgressMonitor());
        }
        if (structType == null || structType.getTypeID() != Types.STRUCT) {
            // The resolved table attribute may be a fallback type for a temporary row type.
            return type instanceof JDBCColumnMetaData || PostgreConstants.TYPE_RECORD.equals(type.getTypeName())
                ? convertToDynamicStruct(session, type, object, copy) : object;
        }
        try {
            if (object == null) {
                return new JDBCCompositeStatic(session, structType, null);
            } else if (object instanceof JDBCCompositeStatic jdbcCompositeStatic) {
                return copy ? jdbcCompositeStatic.cloneValue(session.getProgressMonitor()) : object;
            } else {
                Object value;
                boolean isPgObject = PostgreUtils.isPgObject(dataSource, object);
                if (isPgObject) {
                    value = PostgreUtils.extractPGObjectValue(object, dataSource);
                } else {
                    value = object.toString();
                }
                return convertStringToStruct(session, structType, CommonUtils.toString(value));
            }
        } catch (DBException e) {
            throw new DBCException("Error converting string to composite type", e, session.getExecutionContext());
        }
    }

    @Nullable
    private Object convertToDynamicStruct(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy
    ) throws DBCException {
        boolean anonymousRecord = PostgreConstants.TYPE_RECORD.equals(type.getTypeName());
        if (!anonymousRecord
            && (!(type instanceof JDBCColumnMetaData column)
                || column.getTypeID() != Types.STRUCT
                || !(session instanceof JDBCSession))) {
            return object;
        }
        if (object == null) {
            return null;
        }
        if (object instanceof JDBCComposite composite) {
            return copy ? composite.cloneValue(session.getProgressMonitor()) : composite;
        }
        try {
            if (object instanceof Struct struct) {
                int attributeCount = struct.getAttributes().length;
                String[] names = anonymousRecord ? null : getDynamicAttributeNames(session, type, attributeCount);
                ResultSetMetaData metaData = anonymousRecord ? getRecordMetadata(session, type, attributeCount) : null;
                return !anonymousRecord && names == null ? object : new PostgreDynamicComposite(session, struct, names, metaData);
            }
            PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();
            Object value = object instanceof String ? object : PostgreUtils.extractPGObjectValue(object, dataSource);
            if (!(value instanceof String string) || !string.startsWith("(") || !string.endsWith(")")) {
                return object;
            }
            String[] attributes = PostgreValueParser.parseSingleObject(string.substring(1, string.length() - 1));
            String[] names = anonymousRecord ? null : getDynamicAttributeNames(session, type, attributes.length);
            ResultSetMetaData metaData = anonymousRecord ? getRecordMetadata(session, type, attributes.length) : null;
            if (!anonymousRecord && names == null) {
                return object;
            }
            Struct struct = new JDBCStructImpl(type.getTypeName(), attributes, string);
            return new PostgreDynamicComposite(session, struct, names, metaData);
        } catch (DBCException | SQLException e) {
            // Preserve the original value if a row cannot be parsed without its database type.
            return object;
        }
    }

    @Nullable
    private String[] getDynamicAttributeNames(@NotNull DBCSession session, @NotNull DBSTypedObject type, int attributeCount) {
        if (!(type instanceof JDBCColumnMetaData column) || !(session instanceof JDBCSession jdbcSession)) {
            return null;
        }
        String[] names = dynamicAttributeNames.get(column);
        if (names == null) {
            names = loadTemporaryTypeAttributeNames(jdbcSession, type.getTypeName(), column.getCatalogName());
            dynamicAttributeNames.put(column, names);
        }
        return names.length > 0 && names.length == attributeCount ? names : null;
    }

    @Nullable
    private ResultSetMetaData getRecordMetadata(@NotNull DBCSession session, @NotNull DBSTypedObject type, int attributeCount) {
        if (!(type instanceof JDBCColumnMetaData column) || !(session instanceof JDBCSession jdbcSession)) {
            return null;
        }
        RowSetMetaDataImpl metaData = recordAttributeMetadata.get(column);
        if (metaData == null) {
            metaData = describeAnonymousRecordAttributes(jdbcSession, column);
            recordAttributeMetadata.put(column, metaData);
        }
        try {
            return metaData.getColumnCount() == attributeCount ? metaData : null;
        } catch (SQLException e) {
            return null;
        }
    }

    @NotNull
    private static RowSetMetaDataImpl describeAnonymousRecordAttributes(@NotNull JDBCSession session, @NotNull JDBCColumnMetaData column) {
        RowSetMetaDataImpl snapshot = new RowSetMetaDataImpl();
        DBCExecutionSource source = column.getSource();
        if (source == null || !(source.getSourceDescriptor() instanceof SQLQuery query)
            || query.getType() != SQLQueryType.SELECT || !CommonUtils.isEmpty(query.getParameters())) {
            return snapshot;
        }
        String sql = query.getText().stripTrailing();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        String alias = DBUtils.getQuotedIdentifier(session.getDataSource(), RECORD_QUERY_ALIAS);
        String columnName = DBUtils.getQuotedIdentifier(session.getDataSource(), column.getLabel());
        String metadataQuery = "SELECT (" + alias + "." + columnName + ").* FROM (" + sql + ") " + alias;
        try (JDBCTransactionSavepoint savepoint = new JDBCTransactionSavepoint(session)) {
            try (JDBCPreparedStatement statement = session.prepareStatement(metadataQuery)) {
                // pgjdbc describes this SELECT without executing it or fetching any rows.
                ResultSetMetaData metaData = statement.getMetaData();
                if (metaData == null) {
                    return snapshot;
                }
                int count = metaData.getColumnCount();
                snapshot.setColumnCount(count);
                for (int i = 1; i <= count; i++) {
                    snapshot.setColumnName(i, metaData.getColumnName(i));
                    snapshot.setColumnTypeName(i, metaData.getColumnTypeName(i));
                    snapshot.setColumnType(i, metaData.getColumnType(i));
                    snapshot.setColumnDisplaySize(i, Math.max(0, metaData.getColumnDisplaySize(i)));
                    snapshot.setPrecision(i, Math.max(0, metaData.getPrecision(i)));
                    snapshot.setScale(i, Math.max(0, metaData.getScale(i)));
                    snapshot.setNullable(i, metaData.isNullable(i));
                }
                return snapshot;
            } catch (SQLException e) {
                try {
                    savepoint.rollback();
                } catch (SQLException rollbackError) {
                    log.error("Can't restore transaction after record metadata lookup", rollbackError);
                }
                // Some SELECTs cannot be wrapped; retain the synthetic names for their records.
                return new RowSetMetaDataImpl();
            }
        } catch (SQLException e) {
            // Skip the lookup if a savepoint cannot be created in a manual transaction.
            return new RowSetMetaDataImpl();
        }
    }

    @NotNull
    private static String[] loadTemporaryTypeAttributeNames(
        @NotNull JDBCSession session,
        @NotNull String typeName,
        @Nullable String catalogName
    ) {
        String[] identifiers = SQLUtils.splitFullIdentifier(
            typeName, ".", session.getDataSource().getSQLDialect().getIdentifierQuoteStrings(), false);
        String name = identifiers.length == 0 ? typeName : identifiers[identifiers.length - 1];
        PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();
        PostgreDatabase database = dataSource.getDatabase(catalogName);
        if (database == null && session.getExecutionContext().getOwnerInstance() instanceof PostgreDatabase owner) {
            database = owner;
        }
        if (database == null) {
            return new String[0];
        }
        try {
            PostgreSchemaMyTemp schema = new PostgreSchemaMyTemp(session, database);
            if (identifiers.length > 1
                && !schema.getName().equals(identifiers[identifiers.length - 2])
                && !PostgreConstants.TEMP_SCHEMA_NAME.equals(identifiers[identifiers.length - 2])) {
                return new String[0];
            }
            List<PostgreTableColumn> attributes = schema.readTableColumns(session, name);
            return attributes.stream().map(PostgreTableColumn::getName).toArray(String[]::new);
        } catch (SQLException | DBException e) {
            // Leave the original value intact when the temporary row type is not accessible.
        }
        return new String[0];
    }

    private static final class PostgreDynamicComposite extends JDBCCompositeDynamic {
        private final String rowString;

        private PostgreDynamicComposite(
            @NotNull DBCSession session,
            @NotNull Struct struct,
            @Nullable String[] names,
            @Nullable ResultSetMetaData metaData
        )
                throws DBCException {
            super(session, struct, metaData, names);
            rowString = struct.toString();
            if (metaData != null) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] instanceof String string && !attributes[i].getDataKind().isComplex()) {
                        try {
                            int typeId = attributes[i].getTypeID();
                            values[i] = typeId == Types.NUMERIC || typeId == Types.DECIMAL
                                ? new BigDecimal(string)
                                : PostgreValueParser.convertStringToValue(session, attributes[i], string);
                        } catch (DBCException | RuntimeException e) {
                            // Preserve a member's textual value if its type cannot be converted.
                        }
                    }
                }
            }
        }

        private PostgreDynamicComposite(@NotNull PostgreDynamicComposite source, @NotNull DBRProgressMonitor monitor)
                throws DBCException {
            super(source, monitor);
            rowString = source.rowString;
            modified = source.modified;
        }

        @NotNull
        @Override
        public PostgreDynamicComposite cloneValue(@NotNull DBRProgressMonitor monitor) throws DBCException {
            return new PostgreDynamicComposite(this, monitor);
        }

        @NotNull
        @Override
        public String getStringRepresentation() {
            return modified ? PostgreValueParser.generateObjectString(getValues()) : rowString;
        }
    }

    @NotNull
    @Override
    public synchronized String getValueDisplayString(
        @NotNull DBSTypedObject column,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (!DBUtils.isNullValue(value) && value instanceof JDBCComposite composite) {
            final StringJoiner output = new StringJoiner(",", "(", ")");

            for (DBSAttributeBase attribute : composite.getAttributes()) {
                final DBDValueHandler handler = DBUtils.findValueHandler(composite.getDataType().getDataSource(), attribute);
                final Object item = composite.getAttributeValue(attribute);
                final String member = getStructMemberDisplayString(attribute, handler, item, format);

                output.add(member);
            }

            return output.toString();
        }

        return super.getValueDisplayString(column, value, format);
    }

    @NotNull
    private static String getStructMemberDisplayString(
        @NotNull DBSTypedObject type,
        @NotNull DBDValueHandler handler,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format) {
        if (DBUtils.isNullValue(value)) {
            return "";
        }

        final String string = handler.getValueDisplayString(type, value, DBDDisplayFormat.NATIVE);

        if (format == DBDDisplayFormat.NATIVE && isQuotingRequired(string)) {
            return '"' + string.replace("\"", "\"\"") + '"';
        }

        return string;
    }

    /**
     * @see <a href="https://www.postgresql.org/docs/current/rowtypes.html#ROWTYPES-IO-SYNTAX">8.16.6. Composite Type Input and Output Syntax</a>
     */
    private static boolean isQuotingRequired(@NotNull String value) {
        if (value.isEmpty()) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            switch (value.charAt(index)) {
                case '(':
                case ')':
                case '"':
                case ',':
                case '\\':
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    private JDBCCompositeStatic convertStringToStruct(@NotNull DBCSession session, @NotNull PostgreDataType compType, @NotNull String value) throws DBException {
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        final Collection<PostgreDataTypeAttribute> attributes = compType.getAttributes(session.getProgressMonitor());
        if (attributes == null) {
            throw new DBException("Composite type '" + compType.getTypeName() + "' has no attributes");
        }
        String[] parsedValues = PostgreValueParser.parseSingleObject(value);
        if (parsedValues.length != attributes.size()) {
            log.debug("Number of attributes (" + attributes.size() + ") doesn't match actual number of parsed strings (" + parsedValues.length + ")");
        }
        Object[] attrValues = new Object[attributes.size()];

        Iterator<PostgreDataTypeAttribute> attrIter = attributes.iterator();
        for (int i = 0; i < parsedValues.length && attrIter.hasNext(); i++) {
            final PostgreDataTypeAttribute itemAttr = attrIter.next();
            attrValues[i] = PostgreValueParser.convertStringToValue(session, itemAttr, parsedValues[i]);
        }

        Struct contents = new JDBCStructImpl(compType.getTypeName(), attrValues, value);
        return new JDBCCompositeStatic(session, compType, contents);
    }

}
