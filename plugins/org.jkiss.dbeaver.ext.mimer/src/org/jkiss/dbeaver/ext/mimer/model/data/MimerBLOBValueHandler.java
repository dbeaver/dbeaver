/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.storage.BytesContentStorage;
import org.jkiss.dbeaver.model.data.storage.TemporaryContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Works around a Mimer SQL JDBC driver limitation: {@code getObject()}/{@code getBlob()}/{@code
 * getBytes()} don't work for a {@code BLOB} column - use {@code getBinaryStream()} instead.
 * Overrides {@code fetchColumnValue} to do exactly that, reading up to {@link
 * ModelPreferences#MEMORY_CONTENT_MAX_SIZE} into memory and spilling anything beyond that to a
 * temp file, checking {@code DBRProgressMonitor#isCanceled()} throughout - the same memory/disk
 * split and cancellation handling {@code JDBCContentBLOB#getContents} already uses for a real
 * {@code Blob}, just applied while reading instead of afterward, since this stream is tied to
 * the current row and can't be read again later the way a real {@code Blob} locator could.
 *
 * @author Mimer Information Technology
 */
public class MimerBLOBValueHandler extends JDBCContentValueHandler {

    private static final Log log = Log.getLog(MimerBLOBValueHandler.class);

    public static final MimerBLOBValueHandler INSTANCE = new MimerBLOBValueHandler();

    // ContentUtils.makeTempFile names the file "<name>-" + System.currentTimeMillis() + ".data" -
    // millisecond resolution, no uniqueness of its own. Core's own equivalent call avoids
    // collisions by including the real Blob object's own hashCode() in <name>; we have no such
    // object (that's the whole workaround), so a monotonic counter stands in instead - two spools
    // landing in the same millisecond (easily possible fetching a page of small-ish values) would
    // otherwise throw FileAlreadyExistsException, confirmed live.
    private static final AtomicLong TEMP_FILE_SEQUENCE = new AtomicLong();

    @Override
    protected DBDContent fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        InputStream stream = resultSet.getBinaryStream(index);
        if (stream == null) {
            return getValueFromObject(session, type, null, false, false);
        }
        try {
            return spoolToContent(session, stream);
        } catch (Exception e) {
            throw new DBCException("Error reading BLOB content", e, session.getExecutionContext());
        }
    }

    @NotNull
    private static MimerContentBLOB spoolToContent(@NotNull DBCSession session, @NotNull InputStream stream) throws IOException {
        DBCExecutionContext context = session.getExecutionContext();
        DBRProgressMonitor monitor = session.getProgressMonitor();
        DBPPlatform platform = DBWorkbench.getPlatform();
        int memoryLimit = platform.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        String encoding = DBValueFormatting.getDefaultBinaryFileEncoding(context.getDataSource());

        MimerContentBLOB content = new MimerContentBLOB(context);
        boolean spooled = false;
        try {
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(Math.min(memoryLimit, buffer.length * 8));
            int total = 0;
            int n;
            while (!monitor.isCanceled() && total < memoryLimit && (n = stream.read(buffer)) >= 0) {
                memoryBuffer.write(buffer, 0, n);
                total += n;
            }
            if (monitor.isCanceled() || total < memoryLimit) {
                content.setStorage(new BytesContentStorage(memoryBuffer.toByteArray(), encoding));
            } else {
                Path tempFile = ContentUtils.createTempContentFile(
                    monitor, platform, "mimer_blob" + TEMP_FILE_SEQUENCE.incrementAndGet());
                try (OutputStream os = Files.newOutputStream(tempFile)) {
                    os.write(memoryBuffer.toByteArray());
                    ContentUtils.copyStreams(stream, -1, os, monitor);
                } catch (IOException e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw e;
                } catch (Throwable e) {
                    ContentUtils.deleteTempFile(tempFile);
                    throw new IOException(e);
                }
                content.setStorage(new TemporaryContentStorage(platform, tempFile, encoding, true));
            }
            spooled = true;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (spooled) {
                    // The spooled storage (possibly a temp file) is otherwise unreachable from here on -
                    // nothing else would ever release it, leaking a temp file on every retry.
                    content.release();
                    throw e;
                }
                // The read/write itself already failed and is propagating its own exception out of the
                // try block - don't let a close() failure on top of that mask it.
                log.debug("Error closing Mimer BLOB stream after a failed read", e);
            }
        }
        return content;
    }
}
