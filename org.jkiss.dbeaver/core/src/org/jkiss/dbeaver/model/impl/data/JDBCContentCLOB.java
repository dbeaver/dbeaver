/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.dbc.DBCContentCharacter;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.Reader;
import java.io.Writer;
import java.io.StringReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;

import net.sf.jkiss.utils.streams.MimeTypes;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentCLOB implements DBCContentCharacter {

    private Clob clob;

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

    public void updateContents(Reader stream, long contentLength, DBRProgressMonitor monitor) throws DBCException {
        if (clob == null) {
            // Update with value controller
            throw new DBCException("LOB value is null");
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

    @Override
    public String toString() {
        return "[CLOB]";
    }
}