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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Conservative estimate for opening a script in the Eclipse text editor. During loading,
 * the input byte array, decoded String and temporary read buffers can coexist. The initial
 * Eclipse CopyOnWriteTextStore retains the decoded String rather than copying it, but its
 * line tracker stores an object per line. The multiplier allows for the file bytes, decoded
 * text, transient copies and editor structures; a bounded sample estimates line overhead.
 * A separate reserve leaves room for the rest of the application.
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

    public static long estimatePeakBytes(long fileBytes) {
        return estimatePeakBytes(fileBytes, 0);
    }

    public static long estimatePeakBytes(long fileBytes, long lines) {
        long contentBytes = saturatingMultiply(Math.max(0, fileBytes), FILE_MULTIPLIER);
        long lineBytes = saturatingMultiply(Math.max(0, lines), BYTES_PER_LINE);
        return contentBytes > Long.MAX_VALUE - lineBytes ? Long.MAX_VALUE : contentBytes + lineBytes;
    }

    public static long estimatePeakBytes(File file, long fileBytes) {
        if (fileBytes < MIN_SAMPLE_FILE_BYTES || fileBytes > Integer.MAX_VALUE) {
            return estimatePeakBytes(fileBytes);
        }
        byte[] sample = new byte[SAMPLE_BYTES];
        long breaks = 0;
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            // Sample the beginning, middle and end: headers or a long INSERT at the start
            // should not determine the estimated line density for the entire script.
            for (long offset : new long[]{0, (fileBytes - SAMPLE_BYTES) / 2, fileBytes - SAMPLE_BYTES}) {
                input.seek(offset);
                input.readFully(sample);
                breaks += countLineBreaks(sample);
            }
        } catch (IOException | SecurityException e) {
            // Metadata is unavailable; the base estimate still lets us warn before loading.
            return estimatePeakBytes(fileBytes);
        }
        long sampledBytes = 3L * SAMPLE_BYTES;
        long lines = 1 + (fileBytes * breaks + sampledBytes - 1) / sampledBytes;
        return estimatePeakBytes(fileBytes, lines);
    }

    static int countLineBreaks(byte[] bytes) {
        int count = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\n' || bytes[i] == '\r' && (i + 1 == bytes.length || bytes[i + 1] != '\n')) {
                count++;
            }
        }
        return count;
    }

    private static long saturatingMultiply(long value, int factor) {
        return value > Long.MAX_VALUE / factor ? Long.MAX_VALUE : value * factor;
    }

    public static long availableBytes(long maxHeap, long allocatedHeap, long freeHeap) {
        long occupied = Math.max(0, allocatedHeap - freeHeap);
        long reserve = Math.max(MIN_RESERVE, maxHeap / 4);
        return Math.max(0, maxHeap - occupied - reserve);
    }

    public static boolean shouldWarn(long fileBytes, long maxHeap, long allocatedHeap, long freeHeap) {
        return shouldWarn(fileBytes, maxHeap, allocatedHeap, freeHeap, estimatePeakBytes(fileBytes));
    }

    public static boolean shouldWarn(long fileBytes, long maxHeap, long allocatedHeap, long freeHeap, long estimatedPeakBytes) {
        // Eclipse reads the whole file into one byte array and keeps document offsets as ints.
        return fileBytes > Integer.MAX_VALUE
            || fileBytes > 0 && estimatedPeakBytes > availableBytes(maxHeap, allocatedHeap, freeHeap);
    }
}
