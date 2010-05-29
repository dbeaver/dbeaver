/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDContentBinary;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import net.sf.jkiss.utils.streams.MimeTypes;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentBLOB extends JDBCContentAbstract implements DBDContentBinary {

    private Blob blob;
    private InputStream stream;
    private long streamLength;

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

    public void release()
    {
        if (stream != null) {
            ContentUtils.close(stream);
            stream = null;
        }
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

    public void updateContents(
        DBDValueController valueController,
        InputStream stream,
        long contentLength,
        DBRProgressMonitor monitor,
        ISQLQueryListener listener)
        throws DBException
    {
        if (blob == null) {
            // Update using value controller
            this.stream = stream;
            this.streamLength = contentLength;
            valueController.updateValueImmediately(this, listener);
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

    public void bindParameter(PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (blob != null) {
                preparedStatement.setBlob(paramIndex, blob);
            } else if (stream != null) {
                preparedStatement.setBinaryStream(paramIndex, stream, streamLength);
            } else {
                preparedStatement.setNull(paramIndex, java.sql.Types.BLOB);
            }
        }
        catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public boolean isNull()
    {
        return blob == null && stream == null;
    }

    @Override
    public String toString() {
        return blob == null && stream == null ? null : "[BLOB]";
    }

}
