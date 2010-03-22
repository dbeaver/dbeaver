package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCCLOB;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Clob;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCCLOB implements DBCCLOB {

    private Clob clob;

    public JDBCCLOB(Clob clob) {
        this.clob = clob;
    }

    public long getLength() throws DBCException {
        try {
            return clob.length();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public String getString(long pos, int length) throws DBCException {
        try {
            return clob.getSubString(pos + 1, length);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public int setString(long pos, String str, int offset, int len) throws DBCException {
        try {
            return clob.setString(pos, str, offset, len);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public Reader getCharacterStream() throws DBCException {
        try {
            return clob.getCharacterStream();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public Writer setCharacterStream(long pos) throws DBCException {
        try {
            return clob.setCharacterStream(pos);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    @Override
    public String toString() {
        return "[CLOB]";
    }
}