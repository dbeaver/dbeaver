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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * Conservative estimate for opening a script in the Eclipse text editor. During loading,
 * the input byte array, decoded String and temporary read buffers can coexist (the
 * {@code 3 * fileBytes} component). The initial Eclipse CopyOnWriteTextStore retains the
 * decoded String rather than copying it, but its line tracker stores an object per line
 * (the {@code 40 * estimatedLines} component). Three bounded samples estimate the line count;
 * the size-only fallback uses zero lines. A separate heap reserve (the greater of 256 MiB
 * and one quarter of the maximum heap) leaves room for the rest of the application:
 * <pre>
 * required  ≈ 3 × fileBytes + 40 × estimatedLines
 * available = max(0, maxHeap - max(0, allocatedHeap - freeHeap) - max(256 MiB, maxHeap / 4))
 * warn      = fileBytes &gt; Integer.MAX_VALUE or (fileBytes &gt; 0 and required &gt; available)
 * </pre>
 * Negative file sizes and line counts are clamped to zero; required-heap arithmetic saturates on overflow.
 * This is only a warning heuristic, not a guarantee that opening the file will succeed.
 */
public final class SQLScriptMemoryEstimate {
    private static final long MIB = 1024L * 1024;
    private static final long MIN_RESERVE = 256 * MIB;
    // Allow for the file's byte array, decoded text and transient buffers during reading.
    // On 100-200 MiB UTF-8 scripts this plus the line allowance remained above the observed
    // Eclipse Document load peak; the separate heap reserve covers other editor services.
    private static final int FILE_MULTIPLIER = 3;
    private static final int SAMPLE_BYTES = 32 * 1024;
    private static final long MIN_SAMPLE_FILE_BYTES = 8 * MIB;
    // Eclipse ListLineTracker stores a Line (two ints and a delimiter reference) and an
    // ArrayList reference per line. Allow for array growth and other per-line structures.
    private static final int BYTES_PER_LINE = 40;

    private SQLScriptMemoryEstimate() {
    }

    /**
     * Confirms whether a local SQL file should be opened in DBeaver.
     *
     * <ol>
     *   <li>Allow inputs that do not refer to an existing local file.</li>
     *   <li>Estimate the heap required to open the file, and allow it if the estimate fits the available budget.</li>
     *   <li>Otherwise, show a warning with three choices:
     *     <ol>
     *       <li>Open in DBeaver anyway: return {@code true}.</li>
     *       <li>Open with the system application: schedule the external open and return {@code false}.</li>
     *       <li>Cancel: return {@code false} without opening the file.</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * @param shell the parent shell for the warning dialog
     * @param input the editor input to inspect
     * @return {@code true} if DBeaver should load the input, {@code false} otherwise
     */
    public static boolean confirmLargeScriptOpen(@NotNull Shell shell, @NotNull IEditorInput input) {
        File file = EditorUtils.getLocalFileFromInput(input);
        if (file == null || !file.isFile()) {
            return true;
        }
        long fileBytes = file.length();
        Runtime runtime = Runtime.getRuntime();
        long maxHeap = runtime.maxMemory();
        long allocatedHeap = runtime.totalMemory();
        long freeHeap = runtime.freeMemory();
        long peakBytes = SQLScriptMemoryEstimate.estimatePeakBytes(file, fileBytes);
        if (!SQLScriptMemoryEstimate.shouldWarn(fileBytes, maxHeap, allocatedHeap, freeHeap, peakBytes)) {
            return true;
        }

        long availableBytes = SQLScriptMemoryEstimate.availableBytes(maxHeap, allocatedHeap, freeHeap);
        MessageDialog dialog = new MessageDialog(
            shell,
            SQLEditorMessages.sql_editor_large_file_title,
            null,
            NLS.bind(
                SQLEditorMessages.sql_editor_large_file_message,
                file.getName(), SQLScriptMemoryEstimate.toMiB(fileBytes), SQLScriptMemoryEstimate.toMiB(peakBytes),
                SQLScriptMemoryEstimate.toMiB(availableBytes)
            ),
            MessageDialog.WARNING,
            2,
            SQLEditorMessages.sql_editor_large_file_open_anyway,
            SQLEditorMessages.sql_editor_large_file_open_externally,
            IDialogConstants.CANCEL_LABEL
        );
        int choice = dialog.open();
        if (choice == 0) {
            return true;
        }
        if (choice == 1) {
            Path path = file.toPath();
            new AbstractJob(SQLEditorMessages.sql_editor_large_file_open_externally) {
                @NotNull
                @Override
                protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                    if (!ShellUtils.openExternalFile(path)) {
                        UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showError(
                            SQLEditorMessages.sql_editor_large_file_title,
                            SQLEditorMessages.sql_editor_large_file_external_error
                        ));
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
        return false;
    }

    /**
     * Converts a nonnegative byte count to whole MiB for the warning dialog.
     *
     * <ol>
     *   <li>Round any partial MiB up so a nonempty file or nonzero heap estimate is not displayed as zero
     *       or understated.</li>
     *   <li>Use the rounded value only for display: rounding also raises the shown available heap by less
     *       than 1 MiB, while the warning decision compares the exact byte counts.</li>
     * </ol>
     *
     * @param bytes the nonnegative size in bytes
     * @return the size in whole MiB, rounded up
     */
    private static long toMiB(long bytes) {
        return bytes / MIB + (bytes % MIB == 0 ? 0 : 1);
    }

    /**
     * Estimates peak heap usage from the file size alone.
     *
     * <ol>
     *   <li>Treat the file as having no known line-count overhead.</li>
     *   <li>Delegate to the two-argument estimate, which accounts for the file content.</li>
     * </ol>
     *
     * @return the size-only peak heap estimate in bytes
     */
    public static long estimatePeakBytes(long fileBytes) {
        return SQLScriptMemoryEstimate.estimatePeakBytes(fileBytes, 0);
    }

    /**
     * Estimates peak heap usage from file size and line count.
     *
     * <ol>
     *   <li>Clamp negative inputs to zero and estimate the two sources of heap usage:
     *     <ol>
     *       <li>Copies and temporary buffers proportional to the file size.</li>
     *       <li>Line-tracker structures proportional to the number of lines.</li>
     *     </ol>
     *   </li>
     *   <li>Add the estimates, saturating at {@link Long#MAX_VALUE} on overflow.</li>
     * </ol>
     *
     * @return the estimated peak heap usage in bytes
     */
    public static long estimatePeakBytes(long fileBytes, long lines) {
        long contentBytes = SQLScriptMemoryEstimate.saturatingMultiply(Math.max(0, fileBytes), FILE_MULTIPLIER);
        long lineBytes = SQLScriptMemoryEstimate.saturatingMultiply(Math.max(0, lines), BYTES_PER_LINE);
        return contentBytes > Long.MAX_VALUE - lineBytes ? Long.MAX_VALUE : contentBytes + lineBytes;
    }

    /**
     * Estimates peak heap usage using bounded samples of the file's line density.
     *
     * <ol>
     *   <li>Use the size-only estimate when the file is below the sampling threshold or exceeds
     *       the document offset limit.</li>
     *   <li>Otherwise, read a bounded sample at three positions:
     *     <ol>
     *       <li>The beginning of the file.</li>
     *       <li>The middle of the file.</li>
     *       <li>The end of the file.</li>
     *     </ol>
     *   </li>
     *   <li>Extrapolate the line count from the sampled line breaks and include its memory cost.</li>
     *   <li>Fall back to the size-only estimate if the file cannot be sampled.</li>
     * </ol>
     *
     * @return the estimated peak heap usage in bytes
     */
    public static long estimatePeakBytes(@NotNull File file, long fileBytes) {
        if (fileBytes < MIN_SAMPLE_FILE_BYTES || fileBytes > Integer.MAX_VALUE) {
            return SQLScriptMemoryEstimate.estimatePeakBytes(fileBytes);
        }
        byte[] sample = new byte[SAMPLE_BYTES];
        long breaks = 0;
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            // Sample the beginning, middle and end: headers or a long INSERT at the start
            // should not determine the estimated line density for the entire script.
            for (long offset : new long[]{0, (fileBytes - SAMPLE_BYTES) / 2, fileBytes - SAMPLE_BYTES}) {
                input.seek(offset);
                input.readFully(sample);
                breaks += SQLScriptMemoryEstimate.countLineBreaks(sample);
            }
        } catch (IOException | SecurityException e) {
            // Metadata is unavailable; the base estimate still lets us warn before loading.
            return SQLScriptMemoryEstimate.estimatePeakBytes(fileBytes);
        }
        long sampledBytes = 3L * SAMPLE_BYTES;
        long lines = 1 + (fileBytes * breaks + sampledBytes - 1) / sampledBytes;
        return SQLScriptMemoryEstimate.estimatePeakBytes(fileBytes, lines);
    }

    static int countLineBreaks(@NotNull byte[] bytes) {
        int count = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\n' || bytes[i] == '\r' && (i + 1 == bytes.length || bytes[i + 1] != '\n')) {
                count++;
            }
        }
        return count;
    }

    /**
     * Multiplies a nonnegative value by a positive factor without overflowing.
     *
     * <ol>
     *   <li>Check whether the product would exceed {@link Long#MAX_VALUE}.</li>
     *   <li>Return {@link Long#MAX_VALUE} in that case; otherwise return the exact product.</li>
     * </ol>
     *
     * @param value the nonnegative value to scale
     * @param factor the positive scaling factor
     * @return the product, saturated at {@link Long#MAX_VALUE}
     */
    private static long saturatingMultiply(long value, int factor) {
        return value > Long.MAX_VALUE / factor ? Long.MAX_VALUE : value * factor;
    }

    /**
     * Calculates the heap budget available after reserving memory for the application.
     *
     * <ol>
     *   <li>Subtract free memory from the allocated heap to estimate current usage.</li>
     *   <li>Reserve the greater of 256 MiB and one quarter of the maximum heap for DBeaver.</li>
     *   <li>Subtract usage and the reserve from the maximum heap, returning zero if none remains.</li>
     * </ol>
     *
     * @return the estimated heap budget available for opening the file, in bytes
     */
    public static long availableBytes(long maxHeap, long allocatedHeap, long freeHeap) {
        long occupied = Math.max(0, allocatedHeap - freeHeap);
        long reserve = Math.max(MIN_RESERVE, maxHeap / 4);
        return Math.max(0, maxHeap - occupied - reserve);
    }

    /**
     * Determines whether to warn based on an estimate that does not read the file.
     *
     * <ol>
     *   <li>Estimate the file's peak usage using its size alone, without reading the file.</li>
     *   <li>Delegate to the overload that compares the estimate with the heap budget.</li>
     * </ol>
     *
     * @return whether opening the file should trigger a memory warning
     */
    public static boolean shouldWarn(long fileBytes, long maxHeap, long allocatedHeap, long freeHeap) {
        return SQLScriptMemoryEstimate.shouldWarn(
            fileBytes, maxHeap, allocatedHeap, freeHeap, SQLScriptMemoryEstimate.estimatePeakBytes(fileBytes)
        );
    }

    /**
     * Determines whether to warn using the supplied peak heap estimate.
     *
     * <ol>
     *   <li>Warn if the file exceeds the maximum size addressable by the document.</li>
     *   <li>Otherwise, warn only when both conditions hold:
     *     <ol>
     *       <li>The file is nonempty.</li>
     *       <li>Its estimated peak usage exceeds the heap budget after the reserve.</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * @return whether opening the file should trigger a memory warning
     */
    public static boolean shouldWarn(long fileBytes, long maxHeap, long allocatedHeap, long freeHeap, long estimatedPeakBytes) {
        // Eclipse reads the whole file into one byte array and keeps document offsets as ints.
        return fileBytes > Integer.MAX_VALUE
            || fileBytes > 0 && estimatedPeakBytes > SQLScriptMemoryEstimate.availableBytes(maxHeap, allocatedHeap, freeHeap);
    }
}
