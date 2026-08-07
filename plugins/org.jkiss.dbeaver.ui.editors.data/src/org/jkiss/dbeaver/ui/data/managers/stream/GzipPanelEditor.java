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
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.data.StringContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.data.IValueController;
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
            IValueController controller = getValueController();
            DBCExecutionContext executionContext = controller != null ? controller.getExecutionContext() : null;
            final org.jkiss.dbeaver.model.preferences.DBPPreferenceStore store = executionContext != null
                ? executionContext.getDataSource().getContainer().getPreferenceStore()
                : org.jkiss.dbeaver.runtime.DBWorkbench.getPlatform().getPreferenceStore();
            final int maxTextBytesPref = store.getInt(org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE) * 1000;
            final int maxBytes = maxTextBytesPref > 0 ? maxTextBytesPref : 10_000_000;

            byte[] rawBytes;
            try (InputStream is = storage.getContentStream()) {
                rawBytes = is == null ? new byte[0] : is.readNBytes(maxBytes + 1);
            }
            if (rawBytes.length == 0) {
                super.primeEditorValue(monitor, control, value);
                return;
            }
            if (rawBytes.length > maxBytes) {
                throw new DBException("Compressed content is too large to decompress");
            }

            byte[] decompressedBytes = decompress(rawBytes, maxBytes);
            String decompressedText = new String(decompressedBytes, StandardCharsets.UTF_8);

            TextEditorPart textEditor = getTextEditor();
            if (textEditor != null) {
                textEditor.setInput(new org.jkiss.dbeaver.ui.editors.StringEditorInput(
                    "Decompressed Content",
                    decompressedText,
                    true,
                    StandardCharsets.UTF_8.name()));
            } else {
                control.setText(decompressedText);
            }
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
            is = new GZIPInputStream(new ByteArrayInputStream(bytes));
        } else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0x78) {
            is = new InflaterInputStream(new ByteArrayInputStream(bytes));
        } else {
            try {
                is = new GZIPInputStream(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                is = new InflaterInputStream(new ByteArrayInputStream(bytes));
            }
        }

        try (InputStream decompStream = is) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = decompStream.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }
}
