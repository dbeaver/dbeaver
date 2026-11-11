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
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ByteNumberFormat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Glue shared between {@link MimerBLOBValueHandler}/{@link MimerCLOBValueHandler}'s {@code
 * spoolToContent} and {@link MimerContentBLOB}/{@link MimerContentCLOB}'s {@code
 * getDisplayString} - the byte-vs-char read loop and storage types genuinely differ between the
 * two and stay separate, but the temp-file-with-cleanup-on-failure, close-with-release-on-failure,
 * and sized-placeholder-text pieces around them don't.
 *
 * @author Mimer Information Technology
 */
final class MimerLobUtil {

    private MimerLobUtil() {
    }

    @FunctionalInterface
    interface ContentWriter {
        void write(@NotNull Path tempFile) throws IOException;
    }

    /**
     * Creates a temp content file and hands it to {@code writer} to fill - deletes it again if
     * {@code writer} throws, same cleanup every caller of {@code ContentUtils.createTempContentFile}
     * otherwise has to do for itself.
     */
    @NotNull
    static Path spoolToTempFile(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPPlatform platform,
        @NotNull String tempFileNamePrefix,
        @NotNull ContentWriter writer
    ) throws IOException {
        Path tempFile = ContentUtils.createTempContentFile(monitor, platform, tempFileNamePrefix);
        try {
            writer.write(tempFile);
        } catch (IOException e) {
            ContentUtils.deleteTempFile(tempFile);
            throw e;
        } catch (Throwable e) {
            ContentUtils.deleteTempFile(tempFile);
            throw new IOException(e);
        }
        return tempFile;
    }

    /**
     * Closes {@code source}; if that fails after content was already spooled, releases it first
     * (otherwise nothing else would ever reach it to release it, leaking a temp file on every
     * retry) before rethrowing. A close failure before anything spooled is logged instead, so it
     * doesn't mask whatever already failed and is propagating out of the read loop.
     */
    static void closeAfterSpooling(
        @NotNull Closeable source,
        boolean spooled,
        @NotNull Runnable releaseContent,
        @NotNull Log log,
        @NotNull String kindLabel
    ) throws IOException {
        try {
            source.close();
        } catch (IOException e) {
            if (spooled) {
                releaseContent.run();
                throw e;
            }
            log.debug("Error closing Mimer " + kindLabel + " stream after a failed read", e);
        }
    }

    @NotNull
    static String sizedPlaceholder(@NotNull String label, long contentLength) {
        return "[" + label + ", " + new ByteNumberFormat(ByteNumberFormat.BinaryPrefix.ISO).format(contentLength) + "]";
    }
}
