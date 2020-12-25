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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.sql.SQLException;

/**
 * JDBCContentChars
 *
 * @author Serge Rider
 */
public class JDBCContentChars extends JDBCContentAbstract implements DBDContentStorage, DBDContentCached {

    private String originalData;
    protected String data;

    public JDBCContentChars(DBCExecutionContext executionContext, String data) {
        super(executionContext);
        this.data = this.originalData = data;
    }

    public JDBCContentChars(JDBCContentChars copyFrom) {
        super(copyFrom);
        this.originalData = copyFrom.originalData;
        this.data = copyFrom.data;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        if (data == null) {
            // Empty content
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ByteArrayInputStream(data.getBytes(getCharset()));
        }
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        if (data == null) {
            // Empty content
            return new StringReader(""); //$NON-NLS-1$
        } else {
            return new StringReader(data);
        }
    }

    @Override
    public long getContentLength() {
        if (data == null) {
            return 0;
        }
        return data.length();
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
    }

    @Override
    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        return this;
    }

    @Override
    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException
    {
        if (storage == null) {
            data = null;
        } else {
            try {
                Reader reader = storage.getContentReader();
                try {
                    StringWriter sw = new StringWriter((int)storage.getContentLength());
                    ContentUtils.copyStreams(reader, storage.getContentLength(), sw, monitor);
                    data = sw.toString();
                }
                finally {
                    ContentUtils.close(reader);
                }
            }
            catch (IOException e) {
                throw new DBCException("IO error while reading content", e);
            }
        }
        this.modified = true;
        return false;
    }

    @Override
    public void resetContents()
    {
        this.data = this.originalData;
        this.modified = false;
    }

    @Override
    public String getCharset()
    {
        return DBValueFormatting.getDefaultBinaryFileEncoding(executionContext.getDataSource());
    }

    @Override
    public JDBCContentChars cloneStorage(DBRProgressMonitor monitor)
    {
        return cloneValue(monitor);
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setString(paramIndex, data);
            } else {
                preparedStatement.setNull(paramIndex, columnType.getTypeID());
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public Object getRawValue() {
        return data;
    }

    @Override
    public boolean isNull()
    {
        return data == null;
    }

    @Override
    public void release()
    {
        this.data = this.originalData;
    }

    @Override
    public boolean equals(Object obj)
    {
        return
            obj instanceof JDBCContentChars &&
            CommonUtils.equalObjects(data, ((JDBCContentChars) obj).data);
    }

    @Override
    public int hashCode() {
        return data == null ? 0 : data.hashCode();
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format) {
        return data;
    }

    @Override
    public JDBCContentChars cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentChars(executionContext, data);
    }

    @Override
    public Object getCachedValue()
    {
        return data;
    }

}
