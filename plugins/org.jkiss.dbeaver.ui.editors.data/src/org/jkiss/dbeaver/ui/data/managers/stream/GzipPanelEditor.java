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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.swt.custom.StyledText;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * GZIP/ZLIB panel editor
 */
public class GzipPanelEditor extends TextPanelEditor {

    private static final Log log = Log.getLog(GzipPanelEditor.class);
    private static final long MAX_DECOMPRESSED_SIZE = 50 * 1024 * 1024; // 50 MB limit against decompression bombs

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @Nullable DBDContent value)
        throws DBException {
        if (value == null || value.isNull()) {
            super.primeEditorValue(monitor, control, value);
            return;
        }

        DBDContentStorage storage = value.getContents(monitor);
        if (storage == null) {
            super.primeEditorValue(monitor, control, value);
            return;
        }

        try {
            ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream();
            try (InputStream is = storage.getContentStream()) {
                if (is != null) {
                    ContentUtils.copyStreams(is, -1, rawBuffer, monitor);
                }
            }
            byte[] rawBytes = rawBuffer.toByteArray();
            if (rawBytes.length == 0) {
                super.primeEditorValue(monitor, control, value);
                return;
            }

            byte[] decompressedBytes = decompress(rawBytes);
            String decompressedText = new String(decompressedBytes, StandardCharsets.UTF_8);

            UIUtils.syncExec(() -> {
                TextEditorPart editor = getTextEditor();
                if (editor != null) {
                    control.setRedraw(false);
                    try {
                        control.setWordWrap(false);
                        IValueController controller = getValueController();
                        boolean isReadOnly = controller != null && controller.isReadOnly();
                        editor.setInput(new StringEditorInput("Decompressed Content", decompressedText, isReadOnly, StandardCharsets.UTF_8.name()));
                    } finally {
                        control.setRedraw(true);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error decompressing BLOB content", e);
            throw new DBException("Error decompressing BLOB content", e);
        }
    }

    @NotNull
    private byte[] decompress(@NotNull byte[] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is;
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B) {
            // GZIP format
            is = new GZIPInputStream(new ByteArrayInputStream(bytes));
        } else if (bytes.length >= 2 && (bytes[0] & 0x0F) == 8 && (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF)) % 31 == 0) {
            // ZLIB format (RFC 1950 validation)
            is = new InflaterInputStream(new ByteArrayInputStream(bytes));
        } else {
            // Fallback: try GZIP first, fallback to InflaterInputStream
            try {
                is = new GZIPInputStream(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                is = new InflaterInputStream(new ByteArrayInputStream(bytes));
            }
        }

        try (InputStream decompStream = is) {
            byte[] buffer = new byte[4096];
            int len;
            long totalRead = 0;
            while ((len = decompStream.read(buffer)) > 0) {
                totalRead += len;
                if (totalRead > MAX_DECOMPRESSED_SIZE) {
                    throw new IOException("Decompressed data exceeds maximum allowed size (50MB)");
                }
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }
}
