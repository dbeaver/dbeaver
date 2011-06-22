/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * JDBCContentCLOB
 *
 * @author Serge Rider
 */
public class JDBCContentCLOB extends JDBCContentLOB implements DBDContent {

    static final Log log = LogFactory.getLog(JDBCContentCLOB.class);

    private Clob clob;
    private Reader tmpReader;

    public JDBCContentCLOB(Clob clob) {
        this.clob = clob;
    }

    public long getLOBLength() throws DBCException {
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
            // Free blob - we don't need it anymore
            try {
                clob.free();
            } catch (Throwable e) {
                log.debug(e);
            } finally {
                clob = null;
            }
        }
        return storage;
    }

    public void release()
    {
        if (tmpReader != null) {
            ContentUtils.close(tmpReader);
            tmpReader = null;
        }
//        if (clob != null) {
//            try {
//                clob.free();
//            } catch (Exception e) {
//                log.warn(e);
//            }
//            clob = null;
//        }
        super.release();
    }

    public void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Try 3 jdbc methods to set character stream
                tmpReader = storage.getContentReader();
                try {
                    preparedStatement.setCharacterStream(
                        paramIndex,
                        tmpReader);
                }
                catch (Throwable e) {
                    if (e instanceof SQLException) {
                        throw (SQLException)e;
                    } else {
                        long streamLength = ContentUtils.calculateContentLength(storage.getContentReader());
                        try {
                            preparedStatement.setCharacterStream(
                                paramIndex,
                                tmpReader,
                                streamLength);
                        }
                        catch (Throwable e1) {
                            if (e1 instanceof SQLException) {
                                throw (SQLException)e1;
                            } else {
                                preparedStatement.setCharacterStream(
                                    paramIndex,
                                    tmpReader,
                                    (int)streamLength);
                            }
                        }
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

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new JDBCContentCLOB(null);
    }

    @Override
    public String toString() {
        return clob == null && storage == null ? null : "[CLOB]";
    }
}
