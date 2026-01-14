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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LogOutputStream extends OutputStream {

    public static final long DEFAULT_MAX_LOG_SIZE = 1024 * 1024 * 10; // 10Mb    
    public static final int DEFAULT_MAX_LOG_FILES_COUNT = 3;
    
    public static final String LOGS_MAX_FILE_SIZE = "logs.files.output.maxSize";
    public static final String LOGS_MAX_FILES_COUNT = "logs.files.output.maxCount";


    /**
     * The File object to store messages.  This value may be null.
     */
    private final File currentLogFile;
    
    private final File logFileLocation;

    /**
     * The Writer to log messages to.
     */
    private volatile FileOutputStream currentLogFileOutput = null;
    private volatile long currentLogSize;

    private volatile long maxLogSize;
    private volatile int maxLogFiles;
    
    private final String logFileName;
    private final String logFileNameExtension;
    private final Predicate<String> logFileNamePattern;
    
    public LogOutputStream(@NotNull File debugLogFile) throws IOException {
        if (debugLogFile.exists() && !debugLogFile.isFile()) {
            throw new IOException(
                "Failed to initialize debug log output due to the target not being a file: " + debugLogFile.getAbsolutePath()
            );
        }

        // Use ModelPReferences because we don't want to trigger platform activation by logger initialization
        final DBPPreferenceStore prefStore = ModelPreferences.getPreferences();
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

        final String logFileNameRegexStr = "^" + Pattern.quote(logFileName) + "\\-[0-9]+" + Pattern.quote(logFileNameExtension) + "$";
        this.logFileNamePattern = Pattern.compile(logFileNameRegexStr).asMatchPredicate();
        
        if (debugLogFile.exists()) {
            this.currentLogSize = this.currentLogFile.length();
            this.rotateCurrentLogFile(true);
        } else {
            this.currentLogSize = 0;
            if (this.logFileLocation.mkdirs()) {
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
            
            File newFile = new File(this.logFileLocation, this.logFileName + "-" + System.currentTimeMillis() + this.logFileNameExtension);
            if (!this.currentLogFile.renameTo(newFile)) {
                return false;
            }
            this.currentLogSize = 0;
            
            File[] logFiles = this.logFileLocation.listFiles((File dir, String name) -> this.logFileNamePattern.test(name));
            if (logFiles == null) {
                return false;
            }
            Arrays.sort(logFiles, Comparator.comparing(File::getName));
            for (int i = 0, count = logFiles.length; i < logFiles.length && count > maxLogFiles; i++, count--) {
                logFiles[i].delete();
            }
            
            return true;
        } else {
            return false;
        }
    }
    
}
