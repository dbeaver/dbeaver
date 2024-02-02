/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPLogLocations;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

public class LogOutputStream extends OutputStream {

    public static final long DEFAULT_MAX_LOG_SIZE = 1024 * 1024 * 10; // 10Mb    
    public static final int DEFAULT_MAX_LOG_FILES_COUNT = 3;
    
    public static final String LOGS_MAX_FILE_SIZE = "logs.files.output.maxSize";
    public static final String LOGS_MAX_FILES_COUNT = "logs.files.output.maxCount";

    /**
     * The File object to store messages.  This value may be null.
     */
    private final File currentLogFile;
    private final DBPLogLocations logLocations;

    /**
     * The Writer to log messages to.
     */
    private volatile FileOutputStream currentLogFileOutput = null;
    private volatile long currentLogSize;

    private volatile long maxLogSize;
    private volatile int maxLogFiles;

    public LogOutputStream(@NotNull DBPLogLocations logLocations) throws IOException {
        this.logLocations = logLocations;

        File debugLogFile = logLocations.getDebugLog();
        if (debugLogFile.exists() && !debugLogFile.isFile()) {
            throw new IOException(
                "Failed to initialize debug log output due to the target not being a file: " + debugLogFile.getAbsolutePath()
            );
        }

        final DBPPreferenceStore prefStore = ModelPreferences.getPreferences();
        this.currentLogFile = debugLogFile;
        this.maxLogSize = prefStore.getLong(LOGS_MAX_FILE_SIZE);
        this.maxLogFiles = prefStore.getInt(LOGS_MAX_FILES_COUNT);

        if (debugLogFile.exists()) {
            this.currentLogSize = this.currentLogFile.length();
            this.rotateCurrentLogFile(true);
        } else {
            this.currentLogSize = 0;
            File logFileFolder = logLocations.getDebugLogFolder();
            if (logFileFolder == null || logFileFolder.mkdirs()) {
                throw new IOException("Failed to initialize debug log output location: " + debugLogFile);
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

    private OutputStream getLogFileWriter() throws IOException {
        if (this.currentLogFileOutput == null || this.rotateCurrentLogFile(false)) {
            this.currentLogFileOutput = new FileOutputStream(this.currentLogFile, true);
        }
        return this.currentLogFileOutput;
    }

    /**
     * Checks the log file size. If the log file size reaches the limit then the log is rotated
     * @return false if the file doesn't exist or the log files doesn't need to be rotated
     */
    private boolean rotateCurrentLogFile(boolean force) throws IOException {
        if ((this.currentLogFileOutput != null || this.currentLogFile.exists()) // if we are initializing log file for new launch
            && (this.currentLogSize > this.maxLogSize || force)
        ) {
            this.close();
            
            File newFile = logLocations.proposeDebugLogRotation();
            if (!this.currentLogFile.renameTo(newFile)) {
                return false;
            }
            this.currentLogSize = 0;
            
            List<File> logFiles = logLocations.getDebugLogFiles();
            logFiles.sort(Comparator.comparing(File::getName));
            for (int i = 0, count = logFiles.size(); i < logFiles.size() && count > maxLogFiles; i++, count--) {
                logFiles.get(i).delete();
            }
            return true;
        } else {
            return false;
        }
    }
}
