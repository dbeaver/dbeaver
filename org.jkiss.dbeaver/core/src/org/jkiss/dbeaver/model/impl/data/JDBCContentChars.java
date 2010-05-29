/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentChars extends JDBCContentAbstract implements DBDContentCharacter {

    static Log log = LogFactory.getLog(JDBCContentChars.class);

    private String data;

    public JDBCContentChars(String data) {
        this.data = data;
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

    public void updateContents(
        DBDValueController valueController,
        Reader stream,
        long contentLength,
        DBRProgressMonitor monitor,
        ISQLQueryListener listener)
        throws DBException
    {
        if (stream == null) {
            data = null;
        } else {
            char[] buffer = new char[(int) contentLength];
            try {
                int count = stream.read(buffer);
                if (count != contentLength) {
                    log.warn("Actual content length (" + count + ") is less than declared (" + contentLength + ")");
                }
            }
            catch (IOException e) {
                throw new DBCException("IO error", e);
            }
            data = new String(buffer);
        }
        valueController.updateValueImmediately(this, listener);
    }

    public void bindParameter(PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
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

    @Override
    public String toString() {
        return data;
    }

}