/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.storage.BytesContentStorage;
import org.jkiss.dbeaver.model.data.storage.TemporaryContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * JDBCContentBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentBLOB extends JDBCContentLOB {

    private static final Log log = Log.getLog(JDBCContentBLOB.class);

    private Blob blob;
    private InputStream tmpStream;

    public JDBCContentBLOB(DBCExecutionContext dataSource, Blob blob) {
        super(dataSource);
        this.blob = blob;
    }

    @Override
    public long getLOBLength() throws DBCException {
        if (blob != null) {
            try {
                return blob.length();
            } catch (Throwable e) {
                throw new DBCException(e, executionContext);
            }
        }
        return 0;
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.OCTET_STREAM;
    }

    @Override
    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && blob != null) {
            long contentLength = getContentLength();
            DBPPlatform platform = executionContext.getDataSource().getContainer().getPlatform();
            if (contentLength < platform.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE)) {
                try {
                    try (InputStream bs = blob.getBinaryStream()) {
                        storage = BytesContentStorage.createFromStream(
                            bs,
                            contentLength,
                            getDefaultEncoding());
                    }
                } catch (IOException e) {
                    throw new DBCException("IO error while reading content", e);
                } catch (Throwable e) {
                    throw new DBCException(e, executionContext);
                }
            } else {
                // Create new local storage
                File tempFile;
                try {
                    tempFile = ContentUtils.createTempContentFile(monitor, platform, "blob" + blob.hashCode());
                }
                catch (IOException e) {
                    throw new DBCException("Can't create temporary file", e);
                }
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    try (InputStream bs = blob.getBinaryStream()) {
                        ContentUtils.copyStreams(bs, contentLength, os, monitor);
                    }
                } catch (IOException e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException("IO error while copying stream", e);
                } catch (Throwable e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new DBCException(e, executionContext);
                }
                this.storage = new TemporaryContentStorage(platform, tempFile, getDefaultEncoding());
            }
            // Free blob - we don't need it anymore
            releaseBlob();
        }
        return storage;
    }

    @Override
    public void release()
    {
        releaseTempStream();
        releaseBlob();
        super.release();
    }

    private void releaseBlob() {
        if (blob != null) {
            try {
                blob.free();
            } catch (Throwable e) {
                // Log as warning only if it is an exception.
                // Errors just spam log
                log.debug("Error freeing BLOB: " + e.getClass().getName() + ": " + e.getMessage());
            }
            blob = null;
        }
    }

    private void releaseTempStream() {
        if (tmpStream != null) {
            ContentUtils.close(tmpStream);
            tmpStream = null;
        }
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Write new blob value
                releaseTempStream();
                tmpStream = storage.getContentStream();
                try {
                    preparedStatement.setBinaryStream(paramIndex, tmpStream);
                }
                catch (Throwable e) {
                    try {
                        if (e instanceof SQLException) {
                            throw (SQLException)e;
                        } else {
                            try {
                                preparedStatement.setBinaryStream(paramIndex, tmpStream, storage.getContentLength());
                            }
                            catch (Throwable e1) {
                                if (e1 instanceof SQLException) {
                                    throw (SQLException)e1;
                                } else {
                                    preparedStatement.setBinaryStream(paramIndex, tmpStream, (int)storage.getContentLength());
                                }
                            }
                        }
                    } catch (SQLFeatureNotSupportedException e1) {
                        // Stream values seems to be unsupported
                        // Let's try bytes
                        int contentLength = (int) storage.getContentLength();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream(contentLength);
                        ContentUtils.copyStreams(tmpStream, contentLength, buffer, session.getProgressMonitor());
                        preparedStatement.setBytes(paramIndex, buffer.toByteArray());
                    }
                }
            } else if (blob != null) {
                try {
                    if (columnType.getDataKind() == DBPDataKind.BINARY) {
                        preparedStatement.setBinaryStream(paramIndex, blob.getBinaryStream());
                    } else {
                        preparedStatement.setBlob(paramIndex, blob);
                    }
                }
                catch (Throwable e0) {
                    // Write new blob value
                    releaseTempStream();
                    tmpStream = blob.getBinaryStream();
                    try {
                        preparedStatement.setBinaryStream(paramIndex, tmpStream);
                    }
                    catch (Throwable e) {
                        if (e instanceof SQLException) {
                            throw (SQLException)e;
                        } else {
                            try {
                                preparedStatement.setBinaryStream(paramIndex, tmpStream, blob.length());
                            }
                            catch (Throwable e1) {
                                if (e1 instanceof SQLException) {
                                    throw (SQLException)e1;
                                } else {
                                    preparedStatement.setBinaryStream(paramIndex, tmpStream, (int)blob.length());
                                }
                            }
                        }
                    }
                }
            } else {
                preparedStatement.setNull(paramIndex, java.sql.Types.BLOB);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        catch (Throwable e) {
            throw new DBCException("Error while reading content", e);
        }
    }

    @Override
    public Object getRawValue() {
        return blob;
    }

    @Override
    public boolean isNull()
    {
        return blob == null && storage == null;
    }

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new JDBCContentBLOB(executionContext, null);
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        if (blob == null && storage == null) {
            return null;
        }
        if (storage != null && storage instanceof DBDContentCached) {
            final Object cachedValue = ((DBDContentCached) storage).getCachedValue();
            if (cachedValue instanceof byte[]) {
                return DBValueFormatting.formatBinaryString(executionContext.getDataSource(), (byte[]) cachedValue, format);
            }
        }
        return "[BLOB]";
    }

}
