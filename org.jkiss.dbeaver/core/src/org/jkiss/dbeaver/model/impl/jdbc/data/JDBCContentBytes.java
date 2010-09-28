/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentBytes extends JDBCContentAbstract implements DBDContent, DBDValueClonable, DBDContentStorage  {

    static final Log log = LogFactory.getLog(JDBCContentBytes.class);

    private byte[] originalData;
    private byte[] data;

    public JDBCContentBytes(byte[] data) {
        this.data = this.originalData = data;
    }

    public InputStream getContentStream()
        throws IOException
    {
        if (data == null) {
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ByteArrayInputStream(data);
        }
    }

    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(
            getContentStream());
    }

    public long getContentLength() {
        if (data == null) {
            return 0;
        }
        return data.length;
    }

    public String getCharset()
    {
        return null;
    }

    public JDBCContentBytes cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return cloneValue(monitor);
    }

    public String getContentType()
    {
        return MimeTypes.OCTET_STREAM;
    }

    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        return this;
    }

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
                        log.warn("Actual content length (" + count + ") is less than declared (" + data.length + ")");
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

    public void bindParameter(
        DBCExecutionContext context,
        PreparedStatement preparedStatement,
        DBSTypedObject columnType,
        int paramIndex)
        throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setBytes(paramIndex, data);
            } else {
                preparedStatement.setNull(paramIndex, columnType.getValueType());
            }
        }
        catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public boolean isNull()
    {
        return data == null;
    }

    public JDBCContentBytes makeNull()
    {
        return new JDBCContentBytes(null);
    }

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
        return "binary [" + data.length + "]";
    }

    public JDBCContentBytes cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentBytes(data);
    }

}