/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentChars extends JDBCContentAbstract implements DBDContentCharacter, DBDValueClonable {

    static Log log = LogFactory.getLog(JDBCContentChars.class);

    public static final int MAX_STRING_LENGTH = 1000;

    private String data;

    public JDBCContentChars(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public long getContentLength() throws DBCException {
        if (data == null) {
            return 0;
        }
        return data.length();
    }

    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
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
                Reader reader = new InputStreamReader(storage.getContentStream(), storage.getCharset());
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

    public String getCharset()
    {
        return null;
    }

    public Reader getContents() throws DBCException {
        if (data == null) {
            // Empty content
            return new StringReader("");
        } else {
            return new StringReader(data);
        }
    }

    public void bindParameter(DBRProgressMonitor monitor, PreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setString(paramIndex, data);
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

    public JDBCContentChars makeNull()
    {
        return new JDBCContentChars(null);
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
        if (data == null) {
            return null;
        }
        if (data.length() > MAX_STRING_LENGTH) {
            return data.substring(0, MAX_STRING_LENGTH) + " ...";
        } else {
            return data;
        }
    }

    public DBDValueClonable cloneValue()
    {
        return new JDBCContentChars(data);
    }

}