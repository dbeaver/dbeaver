/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

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

    static final Log log = Log.getLog(JDBCContentCLOB.class);

    private Clob clob;
    private Reader tmpReader;

    public JDBCContentCLOB(DBPDataSource dataSource, Clob clob) {
        super(dataSource);
        this.clob = clob;
    }

    @Override
    public long getLOBLength() throws DBCException {
        if (clob == null) {
            return 0;
        }
        try {
            return clob.length();
        } catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
    }

    @Override
    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
    }

    @Override
    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && clob != null) {
            long contentLength = getContentLength();
            if (contentLength < DBeaverCore.getGlobalPreferenceStore().getInt(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE)) {
                try {
                    storage = StringContentStorage.createFromReader(clob.getCharacterStream(), contentLength);
                }
                catch (IOException e) {
                    throw new DBCException("IO error while reading content", e);
                } catch (SQLException e) {
                    throw new DBCException(e, dataSource);
                }
            } else {
                // Create new local storage
                IFile tempFile;
                try {
                    tempFile = ContentUtils.createTempContentFile(monitor, "clob" + clob.hashCode());
                }
                catch (IOException e) {
                    throw new DBCException("Can't create temp file", e);
                }
                try {
                    ContentUtils.copyReaderToFile(monitor, clob.getCharacterStream(), contentLength, ContentUtils.DEFAULT_FILE_CHARSET_NAME, tempFile);
                } catch (IOException e) {
                    ContentUtils.deleteTempFile(monitor, tempFile);
                    throw new DBCException("IO error while copying content", e);
                } catch (SQLException e) {
                    ContentUtils.deleteTempFile(monitor, tempFile);
                    throw new DBCException(e, dataSource);
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

    @Override
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

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement,
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
            throw new DBCException(e, dataSource);
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading content", e);
        }
    }

    @Override
    public Object getRawValue() {
        return clob;
    }

    @Override
    public boolean isNull()
    {
        return clob == null && storage == null;
    }

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new JDBCContentCLOB(dataSource, null);
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        return clob == null && storage == null ? null : "[CLOB]";
    }
}
