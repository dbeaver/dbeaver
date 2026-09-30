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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * GZIP/ZLIB panel editor
 */
public class GzipPanelEditor extends TextPanelEditor {

    private static final Log log = Log.getLog(GzipPanelEditor.class);

    public static boolean isGzipHeader(@NotNull byte[] header) {
        return header.length >= 2 && (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B;
    }

    public static boolean isZlibHeader(@NotNull byte[] header) {
        if (header.length < 2) {
            return false;
        }
        int b0 = header[0] & 0xFF;
        int b1 = header[1] & 0xFF;
        return (b0 & 0x0F) == 8 && ((b0 << 8) | b1) % 31 == 0;
    }

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

        IValueController controller = getValueController();
        DBPPreferenceStore store = controller != null && controller.getExecutionContext() != null
            ? controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore()
            : DBWorkbench.getPlatform().getPreferenceStore();
        int maxTextSizeKB = store.getInt(ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE);
        long maxDecompressedSize = (maxTextSizeKB > 0 ? maxTextSizeKB : 100) * 1000L;

        try (InputStream is = storage.getContentStream()) {
            if (is == null) {
                super.primeEditorValue(monitor, control, value);
                return;
            }

            PushbackInputStream pbis = new PushbackInputStream(is, 2);
            byte[] header = pbis.readNBytes(2);
            if (header.length < 2) {
                super.primeEditorValue(monitor, control, value);
                return;
            }
            pbis.unread(header);

            InputStream decompStream;
            if (isGzipHeader(header)) {
                decompStream = new GZIPInputStream(pbis);
            } else if (isZlibHeader(header)) {
                decompStream = new InflaterInputStream(pbis);
            } else {
                try {
                    decompStream = new GZIPInputStream(pbis);
                } catch (IOException e) {
                    pbis.unread(header);
                    decompStream = new InflaterInputStream(pbis);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream in = decompStream) {
                byte[] buffer = new byte[4096];
                int len;
                long totalRead = 0;
                while ((len = in.read(buffer)) > 0) {
                    totalRead += len;
                    if (totalRead > maxDecompressedSize) {
                        throw new DBException("Decompressed content exceeds maximum allowed size (" + (maxDecompressedSize / 1000) + " KB)");
                    }
                    baos.write(buffer, 0, len);
                }
            }

            String decompressedText = baos.toString(StandardCharsets.UTF_8);

            UIUtils.syncExec(() -> {
                TextEditorPart editor = getTextEditor();
                if (editor != null) {
                    control.setRedraw(false);
                    try {
                        control.setWordWrap(false);
                        boolean isReadOnly = controller != null && controller.isReadOnly();
                        editor.setInput(new StringEditorInput("Decompressed Content", decompressedText, isReadOnly, StandardCharsets.UTF_8.name()));
                    } finally {
                        control.setRedraw(true);
                    }
                } else {
                    control.setText(decompressedText);
                }
            });
        } catch (DBException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error decompressing BLOB content", e);
            throw new DBException("Error decompressing BLOB content", e);
        }
    }
}
