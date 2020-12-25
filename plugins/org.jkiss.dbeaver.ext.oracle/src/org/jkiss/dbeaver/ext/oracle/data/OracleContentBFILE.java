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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.storage.BytesContentStorage;
import org.jkiss.dbeaver.model.data.storage.TemporaryContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.BeanUtils;

import java.io.*;

/**
 * BFILE content
 */
public class OracleContentBFILE extends JDBCContentLOB {

    private static final Log log = Log.getLog(OracleContentBFILE.class);

    private Object bfile;
    private String name;
    private boolean opened;

    public OracleContentBFILE(DBCExecutionContext executionContext, Object bfile) {
        super(executionContext);
        this.bfile = bfile;
        if (this.bfile != null) {
            try {
                name = (String) BeanUtils.invokeObjectMethod(
                    bfile,
                    "getName");
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    @Override
    public long getLOBLength() throws DBCException {
        if (bfile != null) {
            boolean openLocally = !opened;
            try {
                if (openLocally) {
                    openFile();
                }
                final Object length = BeanUtils.invokeObjectMethod(
                    bfile,
                    "length");
                if (length instanceof Number) {
                    return ((Number) length).longValue();
                }
            } catch (Throwable e) {
                throw new DBCException("Error when reading BFILE length", e, executionContext);
            } finally {
                if (openLocally) {
                    closeFile();
                }
            }
        }
        return 0;
    }

    private void openFile() throws DBCException {
        if (opened) {
            return;
        }
        try {
            BeanUtils.invokeObjectMethod(bfile, "openFile");
            opened = true;
        } catch (Throwable e) {
            throw new DBCException(e, executionContext);
        }
    }

    private void closeFile() throws DBCException {
        if (!opened) {
            return;
        }
        try {
            BeanUtils.invokeObjectMethod(bfile, "closeFile");
            opened = false;
        } catch (Throwable e) {
            throw new DBCException(e, executionContext);
        }
    }

    private InputStream getInputStream() throws DBCException {
        try {
            return (InputStream) BeanUtils.invokeObjectMethod(
                bfile,
                "getBinaryStream");
        } catch (Throwable e) {
            throw new DBCException("Error when reading BFILE length", e, executionContext);
        }
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
        if (storage == null && bfile != null) {
            try {
                openFile();
                long contentLength = getContentLength();
                DBPPlatform platform = executionContext.getDataSource().getContainer().getPlatform();
                if (contentLength < platform.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE)) {
                    try {
                        try (InputStream bs = getInputStream()) {
                            storage = BytesContentStorage.createFromStream(
                                bs,
                                contentLength,
                                getDefaultEncoding());
                        }
                    } catch (IOException e) {
                        throw new DBCException("IO error while reading content", e);
                    }
                } else {
                    // Create new local storage
                    File tempFile;
                    try {
                        tempFile = ContentUtils.createTempContentFile(monitor, platform, "blob" + bfile.hashCode());
                    } catch (IOException e) {
                        throw new DBCException("Can't create temporary file", e);
                    }
                    try (OutputStream os = new FileOutputStream(tempFile)) {
                        try (InputStream bs = getInputStream()) {
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
            finally {
                closeFile();
            }
        }
        return storage;
    }

    @Override
    public void release()
    {
        releaseBlob();
        super.release();
    }

    private void releaseBlob() {
        if (bfile != null) {
            bfile = null;
        }
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        throw new DBCException("BFILE update not supported");
    }

    @Override
    public Object getRawValue() {
        return bfile;
    }

    @Override
    public boolean isNull()
    {
        return bfile == null && storage == null;
    }

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new OracleContentBFILE(executionContext, null);
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        return bfile == null && storage == null ? null : "[BFILE:" + name + "]";
    }

}
