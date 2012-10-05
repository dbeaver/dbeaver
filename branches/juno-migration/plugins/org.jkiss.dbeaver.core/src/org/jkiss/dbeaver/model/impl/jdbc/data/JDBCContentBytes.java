/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.*;
import java.sql.SQLException;

/**
 * JDBCContentBytes
 *
 * @author Serge Rider
 */
public class JDBCContentBytes extends JDBCContentAbstract implements DBDContent, DBDValueCloneable, DBDContentStorage  {

    static final Log log = LogFactory.getLog(JDBCContentBytes.class);

    private byte[] originalData;
    private byte[] data;

    public JDBCContentBytes(byte[] data) {
        this.data = this.originalData = data;
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
        return null;
    }

    @Override
    public JDBCContentBytes cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return cloneValue(monitor);
    }

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
                throw new DBCException(e);
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
        JDBCExecutionContext context,
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
            throw new DBCException(CoreMessages.model_jdbc_jdbc_error, e);
        }
    }

    @Override
    public boolean isNull()
    {
        return data == null;
    }

    @Override
    public JDBCContentBytes makeNull()
    {
        return new JDBCContentBytes(null);
    }

    @Override
    public void release()
    {
        // Return original data
        this.data = this.originalData;
    }

    @Override
    public String toString() {
        if (data == null) {
            return null;
        }
        return "binary [" + data.length + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public JDBCContentBytes cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentBytes(data);
    }

}