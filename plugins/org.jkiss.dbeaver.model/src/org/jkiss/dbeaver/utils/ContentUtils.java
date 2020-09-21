/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;
    private static final String LOB_DIR = ".lob"; //$NON-NLS-1$

    private static final Log log = Log.getLog(ContentUtils.class);

    static {
        GeneralUtils.BOM_MAP.put(GeneralUtils.DEFAULT_ENCODING, new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF} );
        GeneralUtils.BOM_MAP.put("UTF-16", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        GeneralUtils.BOM_MAP.put("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        GeneralUtils.BOM_MAP.put("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE} );
        GeneralUtils.BOM_MAP.put("UTF-32", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        GeneralUtils.BOM_MAP.put("UTF-32BE", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        GeneralUtils.BOM_MAP.put("UTF-32LE", new byte[] { (byte) 0xFE, (byte) 0xFF, 0x0, 0x0} );
    }


    public static File getLobFolder(DBRProgressMonitor monitor, DBPPlatform application)
        throws IOException
    {
        return application.getTempFolder(monitor, LOB_DIR);
    }

    public static File createTempContentFile(DBRProgressMonitor monitor, DBPPlatform application, String fileName)
        throws IOException
    {
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

    public static File makeTempFile(DBRProgressMonitor monitor, File folder, String name, String extension)
        throws IOException
    {
        name = CommonUtils.escapeFileName(name);
        File tempFile = new File(folder, name + "-" + System.currentTimeMillis() + "." + extension);  //$NON-NLS-1$ //$NON-NLS-2$
        if (!tempFile.createNewFile()){
            throw new IOException(MessageFormat.format(ModelMessages.error_can_create_temp_file, tempFile.getAbsolutePath(), folder.getAbsoluteFile()));
        }
        return tempFile;
    }

    public static void deleteTempFile(DBRProgressMonitor monitor, IFile file)
    {
        try {
            file.delete(true, false, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Can't delete temporary file '" + file.getFullPath().toString() + "'", e);
        }
    }

    public static void saveContentToFile(InputStream contentStream, File file, DBRProgressMonitor monitor)
        throws IOException
    {
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
        throws IOException
    {
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
        throws IOException
    {
        monitor.beginTask("Copy binary content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            byte[] buffer = new byte[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
                if (monitor.isCanceled()) {
                    break;
                }
                int count = inputStream.read(buffer);
                if (count <= 0) {
                    break;
                }
                outputStream.write(buffer, 0, count);
                monitor.worked(STREAM_COPY_BUFFER_SIZE);
            }
        }
        finally {
            monitor.done();
        }
    }

    public static void copyStreams(
        Reader reader,
        long contentLength,
        Writer writer,
        DBRProgressMonitor monitor)
        throws IOException
    {
        monitor.beginTask("Copy character content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
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
        }
        finally {
            monitor.done();
        }
    }

    public static long calculateContentLength(
        File file,
        String charset)
        throws IOException
    {
        return calculateContentLength(
            new FileInputStream(file),
            charset);
    }

    public static long calculateContentLength(
        InputStream stream,
        String charset)
        throws IOException
    {
        return calculateContentLength(
            new InputStreamReader(
                stream,
                charset));
    }

    public static long calculateContentLength(
        Reader reader)
        throws IOException
    {
        try {
            long length = 0;
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                length += count;
            }
            return length;
        }
        finally {
            reader.close();
        }
    }

    public static void close(Closeable closeable)
    {
        try {
            closeable.close();
        }
        catch (IOException e) {
            log.warn("Error closing stream", e);
        }
    }

    public static void copyStreamToFile(DBRProgressMonitor monitor, InputStream inputStream, long contentLength, IFile localFile)
        throws IOException
    {
        //localFile.appendContents(inputStream, true, false, monitor.getNestedMonitor());
        File file = localFile.getLocation().toFile();
        try {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                ContentUtils.copyStreams(inputStream, contentLength, outputStream, monitor);
            }
        }
        finally {
            inputStream.close();
        }
        syncFile(monitor, localFile);

    }

    public static void copyReaderToFile(DBRProgressMonitor monitor, Reader reader, long contentLength, String charset, IFile localFile)
        throws IOException
    {
        try {
            if (charset == null) {
                charset = localFile.getCharset();
            } else {
                localFile.setCharset(charset, monitor.getNestedMonitor());
            }
        }
        catch (CoreException e) {
            log.warn("Can't set content charset", e);
        }
        File file = localFile.getLocation().toFile();
        try {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                Writer writer = new OutputStreamWriter(outputStream, charset == null ? GeneralUtils.DEFAULT_ENCODING : charset);
                ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                writer.flush();
            }
        }
        finally {
            reader.close();
        }
        syncFile(monitor, localFile);
    }

    public static void syncFile(DBRProgressMonitor monitor, IResource localFile) {
        // Sync file with contents
        try {
            localFile.refreshLocal(IFile.DEPTH_ZERO, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Can't synchronize file '" + localFile + "' with contents", e);
        }
    }

    public static IFile getUniqueFile(IFolder folder, String fileName, String fileExt)
    {
        IFile file = folder.getFile(fileName + "." + fileExt);
        int index = 1;
        while (file.exists()) {
            file = folder.getFile(fileName + "-" + index + "." + fileExt);
            index++;
        }
        return file;
    }

    public static String readFileToString(File file) throws IOException
    {
        try (InputStream fileStream = new FileInputStream(file)) {
            UnicodeReader unicodeReader = new UnicodeReader(fileStream, StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder((int) file.length());
            char[] buffer = new char[4000];
            for (;;) {
                int count = unicodeReader.read(buffer);
                if (count <= 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString();
        }
    }

    public static String readToString(InputStream is, Charset charset) throws IOException
    {
        return IOUtils.readToString(new UnicodeReader(is, charset));
    }

    @Nullable
    public static IFile convertPathToWorkspaceFile(IPath path)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = root.getFileForLocation(path);
        if (file != null) {
            return file;
        }
        // Probably we have a path to some linked resource
        IPath folderPath = path.removeLastSegments(1);
        URI folderURI = folderPath.toFile().toURI();
        IContainer[] containers = root.findContainersForLocationURI(folderURI);
        if (!ArrayUtils.isEmpty(containers)) {
            IContainer container = containers[0];
            file = container.getFile(path.removeFirstSegments(path.segmentCount() - 1));
        }
        return file;
    }

    @Nullable
    public static IPath convertPathToWorkspacePath(IPath path)
    {
        IFile wFile = convertPathToWorkspaceFile(path);
        return wFile == null ? null : wFile.getFullPath();
    }

    public static boolean isTextContent(DBDContent content)
    {
        String contentType = content == null ? null : content.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ENGLISH).startsWith("text");
    }

    public static boolean isTextMime(String mimeType)
    {
        return mimeType != null && mimeType.toLowerCase(Locale.ENGLISH).startsWith("text");
    }

    public static boolean isTextValue(Object value)
    {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence) {
            return true;
        }
        if (value instanceof byte[]) {
            for (byte b : (byte[])value) {
                if (!Character.isLetterOrDigit(b) && !Character.isSpaceChar(b) && !Character.isISOControl(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isXML(DBDContent content)
    {
        return MimeTypes.TEXT_XML.equalsIgnoreCase(content.getContentType());
    }

    public static boolean isJSON(DBDContent content)
    {
        return MimeTypes.TEXT_JSON.equalsIgnoreCase(content.getContentType());
    }

    @NotNull
    public static String getContentStringValue(@NotNull DBRProgressMonitor monitor, @NotNull DBDContent object) throws DBCException {
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

    @NotNull
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

    public static void checkFolderExists(IFolder folder)
            throws DBException
    {
        checkFolderExists(folder, new VoidProgressMonitor());
    }

    public static void checkFolderExists(IFolder folder, DBRProgressMonitor monitor)
            throws DBException
    {
        if (!folder.exists()) {
            try {
                folder.create(true, true, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't create folder '" + folder.getFullPath() + "'", e);
            }
        }
    }

    public static void makeFileBackup(IFile file) {
        if (!file.exists()) {
            return;
        }
        String backupFileName = file.getName() + ".bak";
        if (!backupFileName.startsWith(".")) {
            backupFileName = "." + backupFileName;
        }
        IFile backupFile = file.getParent().getFile(new Path(backupFileName));
        if (backupFile.exists()) {
            Date backupTime = new Date(backupFile.getModificationStamp());
            if (CommonUtils.isSameDay(backupTime, new Date())) {
                return;
            }
        }
        try (InputStream fis = file.getContents()) {

            if (!backupFile.exists()) {
                backupFile.create(fis, IResource.HIDDEN | IResource.TEAM_PRIVATE, new NullProgressMonitor());
            } else {
                backupFile.setContents(fis, IResource.HIDDEN | IResource.TEAM_PRIVATE, new NullProgressMonitor());
            }
        } catch (Exception e) {
            log.error("Error creating backup copy of " + file.getFullPath(), e);
        }
    }
}
