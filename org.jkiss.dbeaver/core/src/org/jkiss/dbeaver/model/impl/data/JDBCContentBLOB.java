/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.dbc.DBCContentBinary;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;

import net.sf.jkiss.utils.streams.MimeTypes;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentBLOB implements DBCContentBinary {

    private Blob blob;

    public JDBCContentBLOB(Blob blob) {
        this.blob = blob;
    }

    public long getContentLength() throws DBCException {
        if (blob == null) {
            return 0;
        }
        try {
            return blob.length();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public String getContentType()
    {
        return MimeTypes.OCTET_STREAM;
    }

    public InputStream getContents() throws DBCException {
        if (blob == null) {
            // Empty content
            return new ByteArrayInputStream(new byte[0]);
        }
        try {
            return blob.getBinaryStream();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public void updateContents(InputStream stream, long contentLength, DBRProgressMonitor monitor) throws DBCException {
        if (blob == null) {
            // Update using value controller
        } else {
            // Update BLOB directly
            try {
                blob.truncate(0);
                OutputStream blobStream = blob.setBinaryStream(0);
                try {
                    ContentUtils.copyStreams(stream, contentLength, blobStream, monitor);
                }
                finally {
                    blobStream.close();
                }
            } catch (SQLException e) {
                throw new DBCException("JDBC error", e);
            }
            catch (IOException e) {
                throw new DBCException("Error writing stream into BLOB", e);
            }
        }
    }

    @Override
    public String toString() {
        return "[BLOB]";
    }
}
