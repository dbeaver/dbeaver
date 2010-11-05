/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCContentCLOB
 *
 * @author Serge Rider
 */
public class JDBCContentCLOB extends JDBCContentAbstract implements DBDContent {

    static final Log log = LogFactory.getLog(JDBCContentCLOB.class);

    private Clob clob;
    private DBDContentStorage storage;
    private Reader tmpReader;

    public JDBCContentCLOB(Clob clob) {
        this.clob = clob;
    }

    public long getContentLength() throws DBCException {
        if (storage != null) {
            return storage.getContentLength();
        }
        if (clob == null) {
            return 0;
        }
        try {
            return clob.length();
        } catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
    }

    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && clob != null) {
            long contentLength = getContentLength();
            if (contentLength < DBeaverCore.getInstance().getGlobalPreferenceStore().getInt(PrefConstants.MEMORY_CONTENT_MAX_SIZE)) {
                try {
                    storage = StringContentStorage.createFromReader(clob.getCharacterStream(), contentLength);
                }
                catch (Exception e) {
                    throw new DBCException(e);
                }
            } else {
                // Create new local storage
                IFile tempFile;
                try {
                    tempFile = ContentUtils.createTempContentFile(monitor, "clob" + clob.hashCode());
                }
                catch (IOException e) {
                    throw new DBCException(e);
                }
                try {
                    ContentUtils.copyReaderToFile(monitor, clob.getCharacterStream(), contentLength, ContentUtils.DEFAULT_FILE_CHARSET, tempFile);
                } catch (Exception e) {
                    ContentUtils.deleteTempFile(monitor, tempFile);
                    throw new DBCException(e);
                }
                this.storage = new TemporaryContentStorage(tempFile);
            }
        }
        return storage;
    }

    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException
    {
        release();
        this.storage = storage;
        return true;
    }

    public void release()
    {
        if (tmpReader != null) {
            ContentUtils.close(tmpReader);
            tmpReader = null;
        }
        if (storage != null) {
            storage.release();
            storage = null;
        }
    }

    public void bindParameter(DBCExecutionContext context, PreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Try 3 jdbc methods to set character stream
                Reader streamReader = storage.getContentReader();
                try {
                    preparedStatement.setCharacterStream(
                        paramIndex,
                        streamReader);
                }
                catch (AbstractMethodError e) {
                    long streamLength = ContentUtils.calculateContentLength(storage.getContentReader());
                    try {
                        preparedStatement.setCharacterStream(
                            paramIndex,
                            streamReader,
                            streamLength);
                    }
                    catch (AbstractMethodError e1) {
                        preparedStatement.setCharacterStream(
                            paramIndex,
                            streamReader,
                            (int)streamLength);
                    }
                }
            } else if (clob != null) {
                preparedStatement.setClob(paramIndex, clob);
            } else {
                preparedStatement.setNull(paramIndex + 1, java.sql.Types.CLOB);
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
        return clob == null && storage == null;
    }

    public JDBCContentCLOB makeNull()
    {
        return new JDBCContentCLOB(null);
    }

    @Override
    public String toString() {
        return clob == null && storage == null ? null : "[CLOB]";
    }

    public DBDValueClonable cloneValue(DBRProgressMonitor monitor)
        throws DBCException
    {
        JDBCContentCLOB copy = new JDBCContentCLOB(null);
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
