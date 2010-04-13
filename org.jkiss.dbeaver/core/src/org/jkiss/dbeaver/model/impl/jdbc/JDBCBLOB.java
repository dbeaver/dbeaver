/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCBLOB;
import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.Blob;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCBLOB implements DBCBLOB {

    private Blob blob;

    public JDBCBLOB(Blob blob) {
        this.blob = blob;
    }

    public long getLength() throws DBCException {
        try {
            return blob.length();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public byte[] getBytes(long pos, int length) throws DBCException {
        try {
            return blob.getBytes(pos, length);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public int setBytes(long pos, byte[] bytes, int offset, int len) throws DBCException {
        try {
            return blob.setBytes(pos, bytes, offset, len);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public InputStream getBinaryStream() throws DBCException {
        try {
            return blob.getBinaryStream();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public OutputStream setBinaryStream(long pos) throws DBCException {
        try {
            return blob.setBinaryStream(pos);
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    @Override
    public String toString() {
        return "[BLOB]";
    }
}
