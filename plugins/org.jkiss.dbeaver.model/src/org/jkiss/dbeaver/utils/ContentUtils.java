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

package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;
    private static final String LOB_DIR = ".lob"; //$NON-NLS-1$

    private static final Log log = Log.getLog(ContentUtils.class);

    public static Path getLobFolder(DBRProgressMonitor monitor, DBPPlatform application)
        throws IOException {
        return application.getTempFolder(monitor, LOB_DIR);
    }

    public static Path createTempContentFile(DBRProgressMonitor monitor, DBPPlatform application, String fileName)
        throws IOException {
        return makeTempFile(
            monitor,
            getLobFolder(monitor, application),
            fileName,
            "data");
/*
        try {
            String charset = application.getPreferenceStore().getString(ModelPreferences.CONTENT_HEX_ENCODING);
            file.setCharset(charset, monitor.getNestedMonitor());
        } catch (CoreException e) {
            log.error("Can't set file charset", e);
        }
        return file;
*/
    }

    public static Path makeTempFile(DBRProgressMonitor monitor, Path folder, String name, String extension)
        throws IOException {
        name = CommonUtils.escapeFileName(name);
        Path tempFile = folder.resolve(name + "-" + System.currentTimeMillis() + "." + extension);  //$NON-NLS-1$ //$NON-NLS-2$
        Files.createFile(tempFile);
        return tempFile;
    }

    public static void saveContentToFile(InputStream contentStream, File file, DBRProgressMonitor monitor)
        throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            copyStreams(contentStream, file.length(), os, monitor);
        }
        // Check for cancel
        if (monitor.isCanceled()) {
            // Delete output file
            if (!file.delete()) {
                log.warn("Can't delete incomplete file '" + file.getAbsolutePath() + "'");
            }
        }
    }

    public static void saveContentToFile(Reader contentReader, File file, String charset, DBRProgressMonitor monitor)
        throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), charset)) {
            copyStreams(contentReader, file.length(), writer, monitor);
        }
        // Check for cancel
        if (monitor.isCanceled()) {
            // Delete output file
            if (!file.delete()) {
                log.warn("Can't delete incomplete file '" + file.getAbsolutePath() + "'");
            }
        }
    }

    public static void copyStreams(
        InputStream inputStream,
        long contentLength,
        OutputStream outputStream,
        DBRProgressMonitor monitor)
        throws IOException {
        monitor.beginTask("Copy binary content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            byte[] buffer = new byte[STREAM_COPY_BUFFER_SIZE];
            long totalCopied = 0;
            NumberFormat nf = new ByteNumberFormat(ByteNumberFormat.BinaryPrefix.ISO);
            String subtaskSuffix = " / " + nf.format(contentLength);
            for (; ; ) {
                if (monitor.isCanceled()) {
                    break;
                }
                int count = inputStream.read(buffer);
                if (count <= 0) {
                    break;
                }
                totalCopied += count;
                outputStream.write(buffer, 0, count);
                monitor.worked(STREAM_COPY_BUFFER_SIZE);
                if (contentLength > 0) {
                    monitor.subTask(nf.format(totalCopied) + subtaskSuffix);
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static void copyStreams(
        Reader reader,
        long contentLength,
        Writer writer,
        DBRProgressMonitor monitor)
        throws IOException {
        monitor.beginTask("Copy character content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (; ; ) {
                if (monitor.isCanceled()) {
                    break;
                }
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                writer.write(buffer, 0, count);
                monitor.worked(STREAM_COPY_BUFFER_SIZE);
            }
        } finally {
            monitor.done();
        }
    }

    public static long calculateContentLength(
        File file,
        String charset)
        throws IOException {
        return calculateContentLength(
            new FileInputStream(file),
            charset);
    }

    public static long calculateContentLength(
        InputStream stream,
        String charset)
        throws IOException {
        return calculateContentLength(
            new InputStreamReader(
                stream,
                charset));
    }

    public static long calculateContentLength(Reader reader) throws IOException {
        try (reader) {
            long length = 0;
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (; ; ) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                length += count;
            }
            return length;
        }
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log.warn("Error closing stream", e);
        }
    }

    public static String readToString(InputStream is, Charset charset) throws IOException {
        return IOUtils.readToString(new UnicodeReader(is, charset));
    }

    public static boolean isTextContent(DBDContent content) {
        String contentType = content == null ? null : content.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ENGLISH).startsWith("text");
    }

    public static boolean isTextMime(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ENGLISH).startsWith("text");
    }

    public static boolean isTextValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence) {
            return true;
        }
        if (value instanceof byte[]) {
            for (byte b : (byte[]) value) {
                if (!Character.isLetterOrDigit(b) && !Character.isSpaceChar(b) && !Character.isISOControl(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isAsciiText(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        for (byte b : bytes) {
            if (b < ' ' || '~' < b) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXML(DBDContent content) {
        return MimeTypes.TEXT_XML.equalsIgnoreCase(content.getContentType());
    }

    public static boolean isJSON(DBDContent content) {
        return MimeTypes.TEXT_JSON.equalsIgnoreCase(content.getContentType());
    }

    @Nullable
    public static String getContentStringValue(@NotNull DBRProgressMonitor monitor, @NotNull DBDContent object) throws DBCException {
        if (object.isNull()) {
            return null;
        }
        DBDContentStorage data = object.getContents(monitor);
        if (data != null) {
            if (data instanceof DBDContentCached) {
                Object cachedValue = ((DBDContentCached) data).getCachedValue();
                if (cachedValue instanceof String) {
                    return (String) cachedValue;
                }
            }
            try {
                Reader contentReader = data.getContentReader();
                if (contentReader != null) {
                    try {
                        StringWriter buf = new StringWriter();
                        ContentUtils.copyStreams(contentReader, object.getContentLength(), buf, monitor);
                        return buf.toString();
                    } finally {
                        IOUtils.close(contentReader);
                    }
                }
            } catch (IOException e) {
                log.debug("Can't extract string from content", e);
            }
        }
        return object.toString();
    }

    @Nullable
    public static byte[] getContentBinaryValue(@NotNull DBRProgressMonitor monitor, @NotNull DBDContent object) throws DBCException {
        DBDContentStorage data = object.getContents(monitor);
        if (data != null) {
            if (data instanceof DBDContentCached) {
                Object cachedValue = ((DBDContentCached) data).getCachedValue();
                if (cachedValue instanceof byte[]) {
                    return (byte[]) cachedValue;
                }
            }
            try {
                InputStream contentStream = data.getContentStream();
                if (contentStream != null) {
                    try {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        ContentUtils.copyStreams(contentStream, object.getContentLength(), buf, monitor);
                        return buf.toByteArray();
                    } finally {
                        IOUtils.close(contentStream);
                    }
                }
            } catch (IOException e) {
                log.debug("Can't extract string from content", e);
            }
        }
        return null;
    }

    public static void deleteTempFile(File tempFile) {
        if (!tempFile.delete()) {
            log.warn("Can't delete temp file '" + tempFile.getAbsolutePath() + "'");
        }
    }

    public static void deleteTempFile(Path tempFile) {
        try {
            Files.delete(tempFile);
        } catch (IOException e) {
            log.warn("Can't delete temp file '" + tempFile.toAbsolutePath() + "'");
        }
    }

    public static boolean deleteFileRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File ch : files) {
                    if (!deleteFileRecursive(ch)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    public static boolean deleteFileRecursive(Path file) {
        if (Files.isDirectory(file)) {
            try (Stream<Path> list = Files.list(file)) {
                List<Path> files = list.toList();
                for (Path ch : files) {
                    if (!deleteFileRecursive(ch)) {
                        return false;
                    }
                }
            } catch (IOException e) {
                log.warn("Error reading directory " + file, e);
            }
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Error deleting file " + file, e);
            return false;
        }
        return true;
    }

    public static void makeFileBackup(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        String backupFileName = file.getFileName().toString() + ".bak";
        if (!backupFileName.startsWith(".")) {
            backupFileName = "." + backupFileName;
        }
        Path backupFile = file.getParent().resolve(backupFileName);
        if (Files.exists(backupFile)) {
            try {
                Date backupTime = new Date(Files.getLastModifiedTime(backupFile).toMillis());
                if (CommonUtils.isSameDay(backupTime, new Date())) {
                    return;
                }
            } catch (IOException e) {
                log.error("Error getting file modified time", e);
            }
        }
        try {
            Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("Error creating backup copy of " + file.toAbsolutePath(), e);
        }
    }

}
