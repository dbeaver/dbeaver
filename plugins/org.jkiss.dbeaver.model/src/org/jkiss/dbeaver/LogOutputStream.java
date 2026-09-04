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
package org.jkiss.dbeaver;

import org.eclipse.osgi.internal.debug.Debug;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LogOutputStream extends OutputStream {

    public static final long DEFAULT_MAX_LOG_SIZE = 1024 * 1024 * 10; // 10 MiB
    public static final int DEFAULT_MAX_LOG_FILES_COUNT = 7;
    
    public static final String LOGS_MAX_FILE_SIZE = "logs.files.output.maxSize";
    public static final String LOGS_MAX_FILES_COUNT = "logs.files.output.maxCount";

    private static final int FILE_OPERATIONS_RETRY_LIMIT = 5;

    private static final Operations REAL_OPERATIONS = new Operations() {
        @NotNull
        @Override
        public DBPPreferenceStore getPreferences() {
            return ModelPreferences.getPreferences();
        }

        @Override
        public boolean exists(@NotNull File file) {
            return file.exists();
        }

        @Override
        public boolean isFile(@NotNull File file) {
            return file.isFile();
        }

        @Override
        public boolean isDirectory(@NotNull File file) {
            return file.isDirectory();
        }

        @Override
        public boolean makeDirectories(@NotNull File directory) {
            return directory.mkdirs();
        }

        @Override
        public long length(@NotNull File file) {
            return file.length();
        }

        @NotNull
        @Override
        public OutputStream openFile(@NotNull File file) throws IOException {
            return new FileOutputStream(file, true);
        }

        @Override
        public boolean rename(@NotNull File source, @NotNull File target) {
            return source.renameTo(target);
        }

        @Nullable
        @Override
        public File[] listFiles(@NotNull File directory, @NotNull FilenameFilter filter) {
            return directory.listFiles(filter);
        }

        @Override
        public boolean delete(@NotNull File file) {
            return file.delete();
        }

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public void threadSleep(@NotNull Duration duration) throws InterruptedException {
            Thread.sleep(duration);
        }

        @Override
        public void debugPrint(@NotNull String message) {
            Debug.println(message);
        }
    };

    private final Operations operations;
    private final File initialLogFile;
    /**
     * The File object to store messages.  This value may NOT be null.
     */
    private File currentLogFile;

    private final File logFileLocation;

    /**
     * The Writer to log messages to.
     */
    private volatile OutputStream currentLogFileOutput = null;
    private volatile long currentLogSize;

    private volatile long maxLogSize;
    private volatile int maxLogFiles;
    
    private final String logFileName;
    private final String logFileNameExtension;
    private final Predicate<String> logFileNamePattern;

    public LogOutputStream(@NotNull File debugLogFile) throws IOException {
        this(debugLogFile, REAL_OPERATIONS);
    }

    public LogOutputStream(@NotNull File debugLogFile, @NotNull Operations operations) throws IOException {
        this.operations = operations;
        if (operations.exists(debugLogFile) && !operations.isFile(debugLogFile)) {
            throw new IOException(
                "Failed to initialize debug log output due to the target not being a file: " + debugLogFile.getAbsolutePath()
            );
        }

        // Use ModelPReferences because we don't want to trigger platform activation by logger initialization
        final DBPPreferenceStore prefStore = operations.getPreferences();
        this.initialLogFile = debugLogFile;
        this.currentLogFile = debugLogFile;
        this.logFileLocation = debugLogFile.getParentFile();
        this.maxLogSize = prefStore.getLong(LOGS_MAX_FILE_SIZE);
        this.maxLogFiles = prefStore.getInt(LOGS_MAX_FILES_COUNT);
        final String fileName = debugLogFile.getName();
        int fnameExtStart = fileName.lastIndexOf('.');
        if (fnameExtStart >= 0) {
            this.logFileName = fileName.substring(0, fnameExtStart);
            this.logFileNameExtension = fileName.substring(fnameExtStart);
        } else {
            this.logFileName = fileName;
            this.logFileNameExtension = "";
        }

        final String logFileNameRegexStr = "^" + Pattern.quote(logFileName) + "\\-[0-9]+(-since)?" + Pattern.quote(logFileNameExtension) + "$";
        this.logFileNamePattern = Pattern.compile(logFileNameRegexStr).asMatchPredicate();
        
        if (operations.exists(debugLogFile)) {
            this.currentLogSize = operations.length(this.currentLogFile);
            this.rotateCurrentLogFile(true);
        } else {
            this.currentLogSize = 0;
            if ((!operations.exists(this.logFileLocation) && !operations.makeDirectories(this.logFileLocation))
                || !operations.isDirectory(this.logFileLocation)) {
                throw new IOException("Failed to initialize debug log output location: " + debugLogFile.getAbsolutePath());
            }
        }
        
        prefStore.addPropertyChangeListener(ev -> {
            if (LOGS_MAX_FILE_SIZE.equals(ev.getProperty())) {
                this.maxLogSize = prefStore.getLong(LOGS_MAX_FILE_SIZE);   
            }
            if (LOGS_MAX_FILES_COUNT.equals(ev.getProperty())) {
                this.maxLogFiles = prefStore.getInt(LOGS_MAX_FILES_COUNT);
            }
        });
    }
    
    @Override
    public synchronized void write(int b) throws IOException {
        this.getLogFileWriter().write(b);
        this.currentLogSize++;
    }
    
    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        this.getLogFileWriter().write(b, off, len);
        this.currentLogSize += len;
    }
    
    @Override
    public synchronized void flush() throws IOException {
        if (this.currentLogFileOutput != null) {
            this.currentLogFileOutput.flush();
        }
    }
    
    @Override
    public synchronized void close() throws IOException {
        if (this.currentLogFileOutput != null) {
            this.currentLogFileOutput.close();
            this.currentLogFileOutput = null;
        }
    }

    private synchronized OutputStream getLogFileWriter() throws IOException {
        if (this.currentLogFileOutput == null || this.rotateCurrentLogFile(false)) {
            this.currentLogFileOutput = this.operations.openFile(this.currentLogFile);
        }
        return this.currentLogFileOutput;
    }

    /**
     * Checks the log file size. If the log file size reaches the limit then the log is rotated
     *
     * @return false if the file doesn't exist or the log files doesn't need to be rotated
     */
    private boolean rotateCurrentLogFile(boolean force) throws IOException {
        if ((this.currentLogFileOutput != null || this.operations.exists(this.currentLogFile)) // if we are initializing log file for new launch
            && (this.currentLogSize > this.maxLogSize || force)
        ) {
            this.close();
            this.queueOutCurrentLogFile();

            File[] logFiles;
            logFiles = this.listLogFiles();
            if (logFiles != null) {
                // it's ok to sort by name only because of the actual timestamp values not changing the amount of digits in a short term
                Arrays.sort(logFiles, Comparator.comparing(File::getName));
                for (int i = 0, count = logFiles.length; i < logFiles.length && count > maxLogFiles; i++, count--) {
                    deleteLogFile(logFiles[i]);
                }
            } else {
                this.operations.debugPrint("Failed to list existing log files to delete excessive ones.");
            }
            
            return true;
        } else {
            return false;
        }
    }

    /**
     * Rename current log file to have a suffix of the current timestamp. Takes a number of attempts.
     * If all attempts fails, then
     *  - switch from default log file name to file name having timestamp and '-since' suffix,
     *  - and don't ever touch the stuck file in the current application run.
     */
    private void queueOutCurrentLogFile() {
        long stamp = this.operations.currentTimeMillis();
        File newFile = new File(this.logFileLocation, this.logFileName + "-" + stamp + this.logFileNameExtension);
        boolean renamed;
        try {
            renamed = Boolean.TRUE.equals(doIOWithRetry(() -> this.operations.rename(this.currentLogFile, newFile)));
        } catch (IOException e) {
            renamed = false;
            this.operations.debugPrint(e.toString());
        }
        if (!renamed || this.currentLogFile != this.initialLogFile) {
            if (!renamed) {
                this.operations.debugPrint("Failed to rename log " + this.currentLogFile.getAbsolutePath() + " file to " + newFile.getAbsolutePath());
            }
            // if failed to rename, then start using suffix to keep sorting intact
            this.currentLogFile = new File(this.logFileLocation, this.logFileName + "-" + stamp + "-since" + this.logFileNameExtension);
        }
        this.currentLogSize = 0;
    }

    /**
     * Delete given file taking a number of attempts.
     */
    private void deleteLogFile(@NotNull File fileToRemove) {
        boolean removed;
        try {
            removed = Boolean.TRUE.equals(doIOWithRetry(() -> this.operations.delete(fileToRemove)));
        } catch (IOException e) {
            removed = false;
            this.operations.debugPrint(e.toString());
        }
        if (!removed) {
            this.operations.debugPrint("Failed to delete " + fileToRemove.getAbsolutePath());
        }
    }

    /**
     * Obtain a list of existing log files in the log file directory taking a number of attempts.
     */
    @Nullable
    private File[] listLogFiles() {
        File[] logFiles;
        try {
            logFiles = doIOWithRetry(new RetryableIO<File[]>() {
                @Nullable
                @Override
                public File[] run() throws IOException {
                    return operations.listFiles(logFileLocation, (File dir, String name) -> logFileNamePattern.test(name));
                }

                @Override
                public boolean isRetryNeeded(@Nullable File[] result) {
                    return operations.isDirectory(logFileLocation) && result == null; // listFiles returns null on IO error, then retry
                }
            });
        } catch (IOException e) {
            this.operations.debugPrint(e.toString());
            logFiles = null;
        }
        return logFiles;
    }

    private interface RetryableIO<T> {

        @Nullable
        T run() throws IOException;

        default boolean isRetryNeeded(@Nullable T result) {
            return Boolean.FALSE.equals(result);
        }
    }

    /**
     * Try to execute given retryable IO operation, potentially throwing an IOException.
     * If the operation produced an IOException or returns result interpreted as unsuccessful, then retry the operation after a timeout.
     */
    @Nullable
    private <T> T doIOWithRetry(@NotNull RetryableIO<T> retryable) throws IOException {
        IOException initialException = null;
        Map<String, IOException> errors = null;
        for (int i = 0; i <= FILE_OPERATIONS_RETRY_LIMIT; i++) {
            T result;
            try {
                result = retryable.run();
                if (!retryable.isRetryNeeded(result)) {
                    return result;
                }
            } catch (IOException e) {
                if (errors == null) {
                    initialException = e;
                    errors = new HashMap<>();
                }
                errors.put(e.toString(), e);
            }
            if (i < FILE_OPERATIONS_RETRY_LIMIT) {
                try {
                    this.operations.threadSleep(Duration.ofMillis(250)); // wait a bit for filesystem to sync before another attempt
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying IO operation", e);
                }
            }
        }
        if (errors != null && errors.size() > 1) {
            this.operations.debugPrint("Multiple exceptions occurred during IO operation retry, propagating the first one.");
        }
        if (initialException != null) {
            throw new IOException("Failed to retry IO operation due to the underlying exception", initialException);
        } else {
            throw new IOException("Failed to retry IO operation due to an unsuccessful result.");
        }
    }

    public interface Operations {

        @NotNull
        DBPPreferenceStore getPreferences();

        boolean exists(@NotNull File file);

        boolean isFile(@NotNull File file);

        boolean isDirectory(@NotNull File file);

        boolean makeDirectories(@NotNull File directory);

        long length(@NotNull File file);

        @NotNull
        OutputStream openFile(@NotNull File file) throws IOException;

        boolean rename(@NotNull File source, @NotNull File target) throws IOException;

        @Nullable
        File[] listFiles(@NotNull File directory, @NotNull FilenameFilter filter) throws IOException;

        boolean delete(@NotNull File file) throws IOException;

        long currentTimeMillis();

        void threadSleep(@NotNull Duration duration) throws InterruptedException;

        void debugPrint(@NotNull String message);
    }
}
