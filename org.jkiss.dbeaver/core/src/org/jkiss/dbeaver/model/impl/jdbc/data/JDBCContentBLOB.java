/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

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
public class JDBCContentBLOB extends JDBCContentAbstract implements DBDContent {

    static final Log log = LogFactory.getLog(JDBCContentBLOB.class);

    private Blob blob;
    private DBDContentStorage storage;
    private InputStream tmpStream;

    public JDBCContentBLOB(Blob blob) {
        this.blob = blob;
    }

    public long getContentLength() throws DBCException {
        if (storage != null) {
            return storage.getContentLength();
        }
        if (blob != null) {
            try {
                return blob.length();
            } catch (SQLException e) {
                throw new DBCException(e);
            }
        }
        return 0;
    }

    public String getContentType()
    {
        return MimeTypes.OCTET_STREAM;
    }

    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && blob != null) {
            // Create new local storage
            IFile tempFile;
            try {
                tempFile = ContentUtils.createTempContentFile(monitor, "blob" + blob.hashCode());
            }
            catch (IOException e) {
                throw new DBCException(e);
            }
            try {
                ContentUtils.copyStreamToFile(monitor, blob.getBinaryStream(), blob.length(), tempFile);
            } catch (Exception e) {
                ContentUtils.deleteTempFile(monitor, tempFile);
                throw new DBCException(e);
            }
            this.storage = new TemporaryContentStorage(tempFile);

        }
        return storage;
    }

    public boolean updateContents(
        DBRProgressMonitor monitor,
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

    public void bindParameter(DBCExecutionContext context, PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Write new blob value
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

    public DBDValueClonable cloneValue(DBRProgressMonitor monitor)
        throws DBCException
    {
        JDBCContentBLOB copy = new JDBCContentBLOB(null);
        DBDContentStorage storage = getContents(monitor);
        try {
            copy.updateContents(monitor, storage.cloneStorage(monitor));
        }
        catch (Exception e) {
            throw new DBCException(e);
        }
        return copy;
    }
}
