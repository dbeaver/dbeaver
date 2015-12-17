/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
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
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

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

    @NotNull
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
            DBPApplication application = dataSource.getContainer().getApplication();
            if (contentLength < application.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE)) {
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
                File tempFile;
                try {
                    tempFile = ContentUtils.createTempContentFile(monitor, application, "clob" + clob.hashCode());
                }
                catch (IOException e) {
                    throw new DBCException("Can't create temp file", e);
                }
                try (Writer os = new FileWriter(tempFile)) {
                    ContentUtils.copyStreams(clob.getCharacterStream(), contentLength, os, monitor);
                } catch (IOException e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException("IO error while copying content", e);
                } catch (SQLException e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException(e, dataSource);
                }
                this.storage = new TemporaryContentStorage(application, tempFile);
            }
            // Free lob - we don't need it anymore
            releaseClob();
        }
        return storage;
    }

    @Override
    public void release()
    {
        releaseTempStream();
        releaseClob();
        super.release();
    }

    private void releaseClob() {
        if (clob != null) {
            try {
                clob.free();
            } catch (Throwable e) {
                // Log as warning only if it is an exception.
                // Errors just spam log
                if (e instanceof Exception) {
                    log.warn(e);
                } else {
                    log.debug(e);
                }
            }
            clob = null;
        }
    }

    private void releaseTempStream() {
        if (tmpReader != null) {
            ContentUtils.close(tmpReader);
            tmpReader = null;
        }
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Try 3 jdbc methods to set character stream
                releaseTempStream();
                tmpReader = storage.getContentReader();
                try {
                    preparedStatement.setCharacterStream(
                        paramIndex,
                        tmpReader);
                }
                catch (Throwable e) {
                    if (e instanceof SQLException && !(e instanceof SQLFeatureNotSupportedException)) {
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
                            if (e1 instanceof SQLException && !(e instanceof SQLFeatureNotSupportedException)) {
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
        if (clob == null && storage == null) {
            return null;
        }
        if (storage != null && storage instanceof DBDContentCached) {
            return CommonUtils.toString(((DBDContentCached) storage).getCachedValue());
        }
        return "[CLOB]";
    }
}
