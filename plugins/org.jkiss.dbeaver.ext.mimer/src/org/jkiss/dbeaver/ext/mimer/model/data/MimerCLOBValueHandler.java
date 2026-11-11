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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@code CLOB}/{@code NCLOB} sibling of {@link MimerBLOBValueHandler}: the same driver
 * limitation applies to all three Mimer SQL LOB types - {@code getObject()}/{@code getClob()}/
 * {@code getString()} don't work, use {@code getCharacterStream()} instead. One handler covers
 * both {@code CLOB} and {@code NCLOB}.
 * <p>
 * Reads up to {@link ModelPreferences#MEMORY_CONTENT_MAX_SIZE} into memory and spills anything
 * beyond that to a temp file, checking {@code DBRProgressMonitor#isCanceled()} throughout - see
 * {@link MimerBLOBValueHandler}'s javadoc for why (the stream is tied to the current row, so this
 * has to happen now rather than being deferred the way a real {@code Clob} locator could be).
 *
 * @author Mimer Information Technology
 */
public class MimerCLOBValueHandler extends JDBCContentValueHandler {

    private static final Log log = Log.getLog(MimerCLOBValueHandler.class);

    public static final MimerCLOBValueHandler INSTANCE = new MimerCLOBValueHandler();

    // See MimerBLOBValueHandler's identical field for why this is needed.
    private static final AtomicLong TEMP_FILE_SEQUENCE = new AtomicLong();

    @Override
    protected DBDContent fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        Reader reader = resultSet.getCharacterStream(index);
        if (reader == null) {
            return getValueFromObject(session, type, null, false, false);
        }
        try {
            return spoolToContent(session.getProgressMonitor(), session, reader);
        } catch (Exception e) {
            throw new DBCException("Error reading CLOB content", e, session.getExecutionContext());
        }
    }

    @Override
    public DBDContent getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object == null) {
            // type.getTypeID() isn't reliable for every Mimer LOB type name (see
            // MimerValueHandlerProvider's javadoc) - the inherited switch would fall through to a
            // binary JDBCContentBytes wrapper for the driver's vendor code. This handler is only
            // ever selected for a genuine CLOB/NCLOB column, so build the right wrapper directly
            // instead of redispatching on an identifier that isn't reliable here. Covers every
            // null path, not just a fetched result-set value - new-row initialization and "Set to
            // NULL" both call this method directly rather than through fetchColumnValue.
            return new MimerContentCLOB(session.getExecutionContext());
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    private static MimerContentCLOB spoolToContent(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCSession session,
        @NotNull Reader reader
    ) throws IOException {
        DBCExecutionContext context = session.getExecutionContext();
        DBPPlatform platform = DBWorkbench.getPlatform();
        int memoryLimit = platform.getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        String encoding = DBValueFormatting.getDefaultBinaryFileEncoding(context.getDataSource());

        MimerContentCLOB content = new MimerContentCLOB(context);
        boolean spooled = false;
        try {
            char[] buffer = new char[8192];
            char[] memoryBuffer = new char[Math.min(memoryLimit, buffer.length * 8)];
            int total = 0;
            int n;
            while (!monitor.isCanceled() && total < memoryLimit && (n = reader.read(buffer)) >= 0) {
                if (total + n > memoryBuffer.length) {
                    memoryBuffer = Arrays.copyOf(memoryBuffer, MimerLobUtil.nextCapacity(memoryBuffer.length, total + n, memoryLimit));
                }
                System.arraycopy(buffer, 0, memoryBuffer, total, n);
                total += n;
            }
            if (monitor.isCanceled() || total < memoryLimit) {
                content.setStorage(new StringContentStorage(new String(memoryBuffer, 0, total)));
            } else {
                int spooledTotal = total;
                char[] spooledBuffer = memoryBuffer;
                Path tempFile = MimerLobUtil.spoolToTempFile(
                    monitor, platform, "mimer_clob" + TEMP_FILE_SEQUENCE.incrementAndGet(),
                    tempFilePath -> {
                        try (Writer os = Files.newBufferedWriter(tempFilePath, Charset.forName(encoding))) {
                            os.write(spooledBuffer, 0, spooledTotal);
                            ContentUtils.copyStreams(reader, -1, os, monitor);
                        }
                    });
                content.setStorage(new TemporaryContentStorage(platform, tempFile, encoding, true));
            }
            spooled = true;
        } finally {
            MimerLobUtil.closeAfterSpooling(reader, spooled, content::release, log, "CLOB");
        }
        return content;
    }
}
