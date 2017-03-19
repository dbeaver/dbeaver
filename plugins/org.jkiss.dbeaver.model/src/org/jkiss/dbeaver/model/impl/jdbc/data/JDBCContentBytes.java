/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.*;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * JDBCContentBytes
 *
 * @author Serge Rider
 */
public class JDBCContentBytes extends JDBCContentAbstract implements DBDContentStorage, DBDContentCached {

    private static final Log log = Log.getLog(JDBCContentBytes.class);

    private byte[] originalData;
    private byte[] data;

    public JDBCContentBytes(DBPDataSource dataSource) {
        super(dataSource);
        this.data = this.originalData = null;
    }

    public JDBCContentBytes(DBPDataSource dataSource, byte[] data) {
        super(dataSource);
        this.data = this.originalData = data;
    }

    public JDBCContentBytes(DBPDataSource dataSource, String data) {
        super(dataSource);
        this.data = this.originalData = DBValueFormatting.getBinaryPresentation(dataSource).toBytes(data);
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        if (data == null) {
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ByteArrayInputStream(data);
        }
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(
            getContentStream());
    }

    @Override
    public long getContentLength() {
        if (data == null) {
            return 0;
        }
        return data.length;
    }

    @Override
    public String getCharset()
    {
        return DBValueFormatting.getDefaultBinaryFileEncoding(dataSource);
    }

    @Override
    public JDBCContentBytes cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return cloneValue(monitor);
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.OCTET_STREAM;
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
                InputStream is = storage.getContentStream();
                try {
                    data = new byte[(int)storage.getContentLength()];
                    int count = is.read(data);
                    if (count != data.length) {
                        log.warn("Actual content length (" + count + ") is less than declared (" + data.length + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                }
                finally {
                    ContentUtils.close(is);
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
    public void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement preparedStatement,
        DBSTypedObject columnType,
        int paramIndex)
        throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setBytes(paramIndex, data);
            } else {
                preparedStatement.setNull(paramIndex, columnType.getTypeID());
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, dataSource);
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
        // Return original data
        this.data = this.originalData;
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        if (data == null) {
            return null;
        }
        return DBValueFormatting.formatBinaryString(dataSource, data, format);
    }

    @Override
    public JDBCContentBytes cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentBytes(dataSource, data);
    }

    @Override
    public Object getCachedValue()
    {
        return data;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj instanceof JDBCContentBytes) {
            byte[] data2 = ((JDBCContentBytes) obj).data;
            if (data == null) return data2 == null;
            if (data2 != null) return Arrays.equals(data, data2);
        }
        return false;
    }
}