/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentCLOB extends JDBCContentAbstract implements DBDContentCharacter {

    private Clob clob;
    private Reader reader;
    private long streamLength;

    public JDBCContentCLOB(Clob clob) {
        this.clob = clob;
    }

    public long getContentLength() throws DBCException {
        if (clob == null) {
            return 0;
        }
        try {
            return clob.length();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
    }

    public void release()
    {
        if (reader != null) {
            ContentUtils.close(reader);
            reader = null;
        }
    }

    public String getCharset()
    {
        return null;
    }

    public Reader getContents() throws DBCException {
        if (clob == null) {
            return new StringReader("");
        }
        try {
            return clob.getCharacterStream();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
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
        if (clob == null) {
            // Update with value controller
            this.reader = stream;
            this.streamLength = contentLength;
            valueController.updateValueImmediately(this, listener);
        }
        try {
            clob.truncate(0);
            Writer clobWriter = clob.setCharacterStream(0);
            try {
                ContentUtils.copyStreams(stream, contentLength, clobWriter, monitor);
            }
            finally {
                clobWriter.close();
            }
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
        catch (IOException e) {
            throw new DBCException("Error writing stream into CLOB", e);
        }
    }

    public void bindParameter(PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (clob != null) {
                preparedStatement.setClob(paramIndex, clob);
            } else if (reader != null) {
                preparedStatement.setCharacterStream(paramIndex, reader, streamLength);
            } else {
                preparedStatement.setNull(paramIndex + 1, java.sql.Types.CLOB);
            }
        }
        catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public boolean isNull()
    {
        return clob == null && reader == null;
    }

    @Override
    public String toString() {
        return clob == null && reader == null ? null : "[CLOB]";
    }

}
