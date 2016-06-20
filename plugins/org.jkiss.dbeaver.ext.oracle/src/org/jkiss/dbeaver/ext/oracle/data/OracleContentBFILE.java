/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
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

    public OracleContentBFILE(DBPDataSource dataSource, Object bfile) {
        super(dataSource);
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
                throw new DBCException("Error when reading BFILE length", e, dataSource);
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
            throw new DBCException(e, dataSource);
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
            throw new DBCException(e, dataSource);
        }
    }

    private InputStream getInputStream() throws DBCException {
        try {
            return (InputStream) BeanUtils.invokeObjectMethod(
                bfile,
                "getBinaryStream");
        } catch (Throwable e) {
            throw new DBCException("Error when reading BFILE length", e, dataSource);
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
                DBPApplication application = dataSource.getContainer().getApplication();
                if (contentLength < application.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE)) {
                    try {
                        try (InputStream bs = getInputStream()) {
                            storage = BytesContentStorage.createFromStream(
                                bs,
                                contentLength,
                                application.getPreferenceStore().getString(ModelPreferences.CONTENT_HEX_ENCODING));
                        }
                    } catch (IOException e) {
                        throw new DBCException("IO error while reading content", e);
                    }
                } else {
                    // Create new local storage
                    File tempFile;
                    try {
                        tempFile = ContentUtils.createTempContentFile(monitor, application, "blob" + bfile.hashCode());
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
                        throw new DBCException(e, dataSource);
                    }
                    this.storage = new TemporaryContentStorage(application, tempFile);
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
        return new OracleContentBFILE(dataSource, null);
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        return bfile == null && storage == null ? null : "[BFILE:" + name + "]";
    }

}
