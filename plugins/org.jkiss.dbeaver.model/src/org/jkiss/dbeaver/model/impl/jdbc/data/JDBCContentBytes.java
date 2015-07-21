/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ModelPreferences;
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

    static final Log log = Log.getLog(JDBCContentBytes.class);

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
        this.data = this.originalData = DBUtils.getBinaryPresentation(dataSource).toBytes(data);
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
        return DBUtils.getDefaultBinaryFileEncoding(dataSource);
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
        return false;
    }

    @Override
    public void resetContents()
    {
        if (this.originalData != null) {
            this.data = this.originalData;
        }
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
        DBDBinaryFormatter formatter = DBUtils.getBinaryPresentation(dataSource);
        int maxLength = dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN);
        // Convert bytes to string
        int length = data.length;
        if (format == DBDDisplayFormat.UI && length > maxLength) {
            length = maxLength;
        }
        String string = formatter.toString(data, 0, length);
        if (length == data.length) {
            return string;
        }
        StringBuilder strValue = new StringBuilder(length + 10);
        strValue.append(string).append("...").append(" [").append(data.length).append("]");
        return strValue.toString();
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