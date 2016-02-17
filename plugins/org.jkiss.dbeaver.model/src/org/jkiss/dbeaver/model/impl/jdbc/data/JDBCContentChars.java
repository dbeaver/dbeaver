/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
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
    private String data;

    public JDBCContentChars(DBPDataSource dataSource, String data) {
        super(dataSource);
        this.data = this.originalData = data;
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
    public String getCharset()
    {
        return DBUtils.getDefaultBinaryFileEncoding(dataSource);
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
    public String getDisplayString(DBDDisplayFormat format) {
        return data;
    }

    @Override
    public JDBCContentChars cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentChars(dataSource, data);
    }

    @Override
    public Object getCachedValue()
    {
        return data;
    }

}
