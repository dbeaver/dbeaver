/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
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

    private static final Log log = Log.getLog(JDBCContentCLOB.class);

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
        } catch (Throwable e) {
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
            DBPPlatform platform = dataSource.getContainer().getPlatform();
            if (contentLength < platform.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE)) {
                try {
                    storage = StringContentStorage.createFromReader(clob.getCharacterStream(), contentLength);
                }
                catch (IOException e) {
                    throw new DBCException("IO error while reading content", e);
                } catch (Throwable e) {
                    throw new DBCException(e, dataSource);
                }
            } else {
                // Create new local storage
                File tempFile;
                try {
                    tempFile = ContentUtils.createTempContentFile(monitor, platform, "clob" + clob.hashCode());
                }
                catch (IOException e) {
                    throw new DBCException("Can't create temp file", e);
                }
                try (Writer os = new OutputStreamWriter(new FileOutputStream(tempFile), getDefaultEncoding())) {
                    ContentUtils.copyStreams(clob.getCharacterStream(), contentLength, os, monitor);
                } catch (IOException e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException("IO error while copying content", e);
                } catch (Throwable e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException(e, dataSource);
                }
                this.storage = new TemporaryContentStorage(platform, tempFile, getDefaultEncoding());
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
                log.debug("Error freeing CLOB: " + e.getClass().getName() + ": " + e.getMessage());
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
//                String stringValue = ContentUtils.getContentStringValue(session.getProgressMonitor(), this);
//                preparedStatement.setString(paramIndex, stringValue);
                // Try 3 jdbc methods to set character stream
                releaseTempStream();
                tmpReader = storage.getContentReader();
                try {
                    preparedStatement.setNCharacterStream(
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
                preparedStatement.setNull(paramIndex, java.sql.Types.CLOB);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
        catch (Throwable e) {
            throw new DBCException("IO error while binding content", e);
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
        if (storage != null) {
            if (storage instanceof DBDContentCached) {
                return CommonUtils.toString(((DBDContentCached) storage).getCachedValue());
            } else {
                if (storage instanceof ExternalContentStorage) {
                    return "[" + ((ExternalContentStorage) storage).getFile().getName() + "]";
                }
            }
        }
        return "[CLOB]";
    }
}
