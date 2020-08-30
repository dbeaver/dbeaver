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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.sql.*;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 *
 * @author Serge Rider
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler implements DBDContentValueHandler {

    private static final Log log = Log.getLog(JDBCContentValueHandler.class);

    public static final JDBCContentValueHandler INSTANCE = new JDBCContentValueHandler();

    public static final int MAX_CACHED_CLOB_LENGTH = 10000;

    @NotNull
    @Override
    public String getValueContentType(@NotNull DBSTypedObject attribute) {
        if (attribute.getTypeID() == Types.SQLXML) {
            return MimeTypes.TEXT_XML;
        }
        return MimeTypes.OCTET_STREAM;
    }

    @Override
    protected DBDContent fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        if (value == null && !resultSet.wasNull()) {
            // This may happen in some bad drivers like ODBC bridge
            switch (type.getTypeID()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    value = resultSet.getString(index);
                    break;
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                case java.sql.Types.BLOB:
                    value = resultSet.getBytes(index);
                    break;
                case java.sql.Types.SQLXML:
                    value = resultSet.getSQLXML(index);
                    break;
                default:
                    value = resultSet.getObject(index);
                    break;
            }
        }
        if (value instanceof String) {
            // If we have a string - do not try to convert it to a binary representation (#494)
            // We need to convert only in case of some value transformations, not when getting it from DB
            return new JDBCContentChars(session.getExecutionContext(), (String) value);
        }
        return getValueFromObject(session, type, value, false, false);
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(session, statement, paramType, paramIndex);
        } else {
            throw new DBCException(ModelMessages.model_jdbc_unsupported_value_type_ + value);
        }
    }

    @NotNull
    @Override
    public Class<DBDContent> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return DBDContent.class;
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            // Create wrapper using column type
            switch (type.getTypeID()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                    return new JDBCContentChars(session.getExecutionContext(), null);
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return new JDBCContentCLOB(session.getExecutionContext(), null);
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(session.getExecutionContext());
                case java.sql.Types.BLOB:
                    return new JDBCContentBLOB(session.getExecutionContext(), null);
                case java.sql.Types.SQLXML:
                    return new JDBCContentXML(session.getExecutionContext(), null);
                default:
                    {
                        String typeName = type.getTypeName();
                        if (typeName.contains(DBConstants.TYPE_NAME_XML) || typeName.contains(DBConstants.TYPE_NAME_XML2)) {
                            return new JDBCContentXML(session.getExecutionContext(), null);
                        } else if (typeName.contains(DBConstants.TYPE_NAME_JSON) || typeName.contains(DBConstants.TYPE_NAME_JSON2)) {
                            return new JDBCContentChars(session.getExecutionContext(), null);
                        } else {
                            //log.debug(ModelMessages.model_jdbc_unsupported_column_type_ + type.getTypeName());
                            return new JDBCContentBytes(session.getExecutionContext());
                        }
                    }
            }
        } else if (object instanceof byte[]) {
            return new JDBCContentBytes(session.getExecutionContext(), (byte[]) object);
        } else if (object instanceof String) {
            // String is a default format in many cases (like clipboard transfer)
            // So it is possible that real object type isn't string
            switch (type.getTypeID()) {
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(session.getExecutionContext(), (String) object);
                default:
                    // String by default
                    return new JDBCContentChars(session.getExecutionContext(), (String) object);
            }
        } else if (object instanceof Number) {
            return new JDBCContentBytes(session.getExecutionContext(), object.toString());
        } else if (object instanceof Blob) {
            final JDBCContentBLOB blob = new JDBCContentBLOB(session.getExecutionContext(), (Blob) object);
            final DBPPreferenceStore preferenceStore = session.getDataSource().getContainer().getPreferenceStore();
            if (preferenceStore.getBoolean(ModelPreferences.CONTENT_CACHE_BLOB) &&
                blob.getLOBLength() < preferenceStore.getLong(ModelPreferences.CONTENT_CACHE_MAX_SIZE))
            {
                // Precache content
                blob.getContents(session.getProgressMonitor());
            }
            return blob;
        } else if (object instanceof Clob) {
            JDBCContentCLOB clob = new JDBCContentCLOB(session.getExecutionContext(), (Clob) object);
            final DBPPreferenceStore preferenceStore = session.getDataSource().getContainer().getPreferenceStore();
            if (preferenceStore.getBoolean(ModelPreferences.CONTENT_CACHE_CLOB) &&
                clob.getLOBLength() < preferenceStore.getLong(ModelPreferences.CONTENT_CACHE_MAX_SIZE))
            {
                // Precache content
                clob.getContents(session.getProgressMonitor());
            }
            return clob;
        } else if (object instanceof SQLXML) {
            return new JDBCContentXML(session.getExecutionContext(), (SQLXML) object);
        } else if (object instanceof InputStream) {
            // Some weird drivers returns InputStream instead of Xlob.
            // Copy stream to byte array
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final InputStream stream = (InputStream) object;
            try {
                IOUtils.copyStream(stream, buffer);
            } catch (Exception e) {
                throw new DBCException("Error reading content stream", e);
            }
            IOUtils.close(stream);
            return new JDBCContentBytes(session.getExecutionContext(), buffer.toByteArray());
        } else if (object instanceof Reader) {
            // Copy reader to string
            StringWriter buffer = new StringWriter();
            final Reader reader = (Reader) object;
            try {
                IOUtils.copyText(reader, buffer);
            } catch (Exception e) {
                throw new DBCException("Error reading content reader", e);
            }
            IOUtils.close(reader);
            return new JDBCContentChars(session.getExecutionContext(), buffer.toString());
        } else if (object instanceof DBDContent) {
            if (copy && object instanceof DBDValueCloneable) {
                return (DBDContent) ((DBDValueCloneable)object).cloneValue(session.getProgressMonitor());
            }
            return (DBDContent) object;
        } else {
            // Give up. Let's show string value
            return new JDBCContentChars(session.getExecutionContext(), CommonUtils.toString(object));
            //throw new DBCException(ModelMessages.model_jdbc_unsupported_value_type_ + object.getClass().getName());
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof DBDContent) {
            String result = ((DBDContent) value).getDisplayString(format);
            if (result == null) {
                return super.getValueDisplayString(column, null, format);
            } else {
                return result;
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Override
    public void writeStreamValue(DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull DBSTypedObject type, @NotNull DBDContent object, @NotNull Writer writer) throws DBCException, IOException {
        DBDContentStorage cs = object.getContents(monitor);
        if (cs != null) {
            if (ContentUtils.isTextContent(object)) {
                writer.write("'");
                String strValue = ContentUtils.getContentStringValue(monitor, object);
                strValue = dataSource.getSQLDialect().escapeString(strValue);
                writer.write(strValue);
                writer.write("'");
            } else {

                {
                    DBDBinaryFormatter binaryFormatter = dataSource.getSQLDialect().getNativeBinaryFormatter();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream((int) cs.getContentLength());
                    try (InputStream contentStream = cs.getContentStream()) {
                        IOUtils.copyStream(contentStream, buffer);
                    }
                    final byte[] bytes = buffer.toByteArray();
                    final String binaryString = binaryFormatter.toString(bytes, 0, bytes.length);
                    writer.write(binaryString);
                }
            }
        } else {
            writer.write("NULL");
        }
    }

}
