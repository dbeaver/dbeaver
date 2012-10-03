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
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.sql.SQLException;

/**
 * JDBCContentChars
 *
 * @author Serge Rider
 */
public class JDBCContentChars extends JDBCContentAbstract implements DBDContent, DBDValueCloneable, DBDContentStorage {

    //static final Log log = LogFactory.getLog(JDBCContentChars.class);

    private String originalData;
    private String data;

    public JDBCContentChars(String data) {
        this.data = this.originalData = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        if (data == null) {
            // Empty content
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ByteArrayInputStream(data.getBytes());
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
    public String getCharset()
    {
        return ContentUtils.DEFAULT_FILE_CHARSET;
    }

    @Override
    public JDBCContentChars cloneStorage(DBRProgressMonitor monitor)
    {
        return cloneValue(monitor);
    }

    @Override
    public void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement preparedStatement,
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
            throw new DBCException(CoreMessages.model_jdbc_jdbc_error, e);
        }
    }

    @Override
    public boolean isNull()
    {
        return data == null;
    }

    @Override
    public JDBCContentChars makeNull()
    {
        return new JDBCContentChars(null);
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
    public String toString() {
        return data;
    }

    @Override
    public JDBCContentChars cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCContentChars(data);
    }

}
