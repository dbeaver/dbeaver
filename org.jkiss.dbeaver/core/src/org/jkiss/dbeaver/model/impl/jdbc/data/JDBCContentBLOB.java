/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.model.data.DBDContentBinary;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentBLOB extends JDBCContentAbstract implements DBDContentBinary {

    static Log log = LogFactory.getLog(JDBCContentBLOB.class);

    private Blob blob;
    private DBDContentStorage storage;
    private InputStream tmpStream;

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

    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDValueController valueController,
        DBDContentStorage storage)
        throws DBException
    {
        this.release();
        this.storage = storage;
        return true;
    }

    public void release()
    {
        if (tmpStream != null) {
            ContentUtils.close(tmpStream);
            tmpStream = null;
        }
        if (storage != null) {
            storage.release();
            storage = null;
        }
    }

    public InputStream getContents() throws DBCException {
        if (storage != null) {
            try {
                return storage.getContentStream();
            }
            catch (IOException e) {
                throw new DBCException(e);
            }
        }
        if (blob != null) {
            try {
                return blob.getBinaryStream();
            } catch (SQLException e) {
                throw new DBCException(e);
            }
        }
        // Empty content
        return new ByteArrayInputStream(new byte[0]);
    }

    public void bindParameter(PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                tmpStream = storage.getContentStream();
                try {
                    preparedStatement.setBinaryStream(paramIndex, tmpStream);
                }
                catch (AbstractMethodError e) {
                    try {
                        preparedStatement.setBinaryStream(paramIndex, tmpStream, storage.getContentLength());
                    }
                    catch (AbstractMethodError e1) {
                        preparedStatement.setBinaryStream(paramIndex, tmpStream, (int)storage.getContentLength());
                    }
                }
            } else if (blob != null) {
                preparedStatement.setBlob(paramIndex, blob);
            } else {
                preparedStatement.setNull(paramIndex, java.sql.Types.BLOB);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
        catch (IOException e) {
            throw new DBCException(e);
        }
    }

    public boolean isNull()
    {
        return blob == null && storage == null;
    }

    public JDBCContentBLOB makeNull()
    {
        return new JDBCContentBLOB(null);
    }

    @Override
    public String toString() {
        return blob == null && storage == null ? null : "[BLOB]";
    }

    public DBDValueClonable cloneValue()
    {
        return null;
    }
}
