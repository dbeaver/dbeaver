/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.CorePrefConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;

    static final Log log = LogFactory.getLog(ContentUtils.class);
    public static final String DEFAULT_FILE_CHARSET_NAME = "UTF-8";

    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;

    private static final Map<String, byte[]> BOM_MAP = new HashMap<String, byte[]>();

    static {
        BOM_MAP.put("UTF-8", new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF} );
        BOM_MAP.put("UTF-16", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE} );
        BOM_MAP.put("UTF-32", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-32BE", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-32LE", new byte[] { (byte) 0xFE, (byte) 0xFF, 0x0, 0x0} );
    }

    static final char[] HEX_CHAR_TABLE = {
      '0', '1', '2', '3',
      '4', '5', '6', '7',
      '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f'
    };
    public static byte[] getCharsetBOM(String charsetName)
    {
        return BOM_MAP.get(charsetName.toUpperCase());
    }

    public static void writeByteAsHex(Writer out, byte b) throws IOException
    {
        int v = b & 0xFF;
        out.write(HEX_CHAR_TABLE[v >>> 4]);
        out.write(HEX_CHAR_TABLE[v & 0xF]);
    }

    public static void writeBytesAsHex(Writer out, byte[] buf, int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            int v = b & 0xFF;
            out.write(HEX_CHAR_TABLE[v >>> 4]);
            out.write(HEX_CHAR_TABLE[v & 0xF]);
        }
    }

    public static String getDefaultFileEncoding()
    {
        return System.getProperty("file.encoding", DEFAULT_FILE_CHARSET_NAME);
    }

    public static String getDefaultLineSeparator()
    {
        return System.getProperty("line.separator", "\n");
    }

    public static String getDefaultBinaryFileEncoding(DBPDataSource dataSource)
    {
        IPreferenceStore preferenceStore;
        if (dataSource == null) {
            preferenceStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
        } else {
            preferenceStore = dataSource.getContainer().getPreferenceStore();
        }
        String fileEncoding = preferenceStore.getString(CorePrefConstants.CONTENT_HEX_ENCODING);
        if (CommonUtils.isEmpty(fileEncoding)) {
            fileEncoding = getDefaultFileEncoding();
        }
        return fileEncoding;
    }

    public static IFile createTempContentFile(DBRProgressMonitor monitor, String fileName)
        throws IOException
    {
        IFile file = makeTempFile(
            monitor,
            DBeaverCore.getInstance().getLobFolder(monitor.getNestedMonitor()),
            fileName,
            "data");
        try {
            file.setCharset(getDefaultBinaryFileEncoding(null), monitor.getNestedMonitor());
        } catch (CoreException e) {
            log.error("Can't set file charset", e);
        }
        return file;
    }

    public static IFile makeTempFile(DBRProgressMonitor monitor, IFolder folder, String name, String extension)
        throws IOException
    {
        IFile tempFile = folder.getFile(name + "-" + System.currentTimeMillis() + "." + extension);  //$NON-NLS-1$ //$NON-NLS-2$
        try {
            InputStream contents = new ByteArrayInputStream(new byte[0]);
            tempFile.create(contents, true, monitor.getNestedMonitor());
        } catch (CoreException ex) {
            throw new IOException(MessageFormat.format(CoreMessages.DBeaverCore_error_can_create_temp_file, tempFile.toString(), folder.toString()), ex);
        }
        return tempFile;
    }

    public static void deleteTempFile(DBRProgressMonitor monitor, IFile file)
    {
        try {
            file.delete(true, false, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Could not delete temporary file '" + file.getFullPath().toString() + "'", e);
        }
    }

    public static void saveContentToFile(InputStream contentStream, File file, DBRProgressMonitor monitor)
        throws IOException
    {
        try {
            OutputStream os = new FileOutputStream(file);
            try {
                copyStreams(contentStream, file.length(), os, monitor);
            }
            finally {
                os.close();
            }
            // Check for cancel
            if (monitor.isCanceled()) {
                // Delete output file
                if (!file.delete()) {
                    log.warn("Could not delete incomplete file '" + file.getAbsolutePath() + "'");
                }
            }
        }
        finally {
            contentStream.close();
        }
    }

    public static void saveContentToFile(Reader contentReader, File file, String charset, DBRProgressMonitor monitor)
        throws IOException
    {
        try {
            Writer writer = new OutputStreamWriter(
                new FileOutputStream(file),
                charset);

            try {
                copyStreams(contentReader, file.length(), writer, monitor);
            }
            finally {
                writer.close();
            }
            // Check for cancel
            if (monitor.isCanceled()) {
                // Delete output file
                if (!file.delete()) {
                    log.warn("Could not delete incomplete file '" + file.getAbsolutePath() + "'");
                }
            }
        }
        finally {
            contentReader.close();
        }
    }

    public static void copyStreams(
        InputStream inputStream,
        long contentLength,
        OutputStream outputStream,
        DBRProgressMonitor monitor)
        throws IOException
    {
        int segmentSize = (int)(contentLength / STREAM_COPY_BUFFER_SIZE);

        monitor.beginTask("Copy binary content", segmentSize);
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
                monitor.worked(1);
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
        int segmentSize = (int)(contentLength / STREAM_COPY_BUFFER_SIZE);

        monitor.beginTask("Copy character content", segmentSize);
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
                monitor.worked(1);
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

    public static boolean isTextContent(DBDContent content)
    {
        String contentType = content.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("text");
    }

    public static void copyStreamToFile(DBRProgressMonitor monitor, InputStream inputStream, long contentLength, IFile localFile)
        throws DBException, IOException
    {
        //localFile.appendContents(inputStream, true, false, monitor.getNestedMonitor());
        File file = localFile.getLocation().toFile();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            try {
                ContentUtils.copyStreams(inputStream, contentLength, outputStream, monitor);
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            inputStream.close();
        }
        syncFile(monitor, localFile);

    }

    public static void copyReaderToFile(DBRProgressMonitor monitor, Reader reader, long contentLength, String charset, IFile localFile)
        throws DBException, IOException
    {
        try {
            if (charset == null) {
                charset = localFile.getCharset();
            } else {
                localFile.setCharset(charset, monitor.getNestedMonitor());
            }
        }
        catch (CoreException e) {
            throw new DBException("Can't handle content charset", e);
        }
        File file = localFile.getLocation().toFile();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            try {
                Writer writer = new OutputStreamWriter(outputStream, charset);
                ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                writer.flush();
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            reader.close();
        }
        syncFile(monitor, localFile);
    }

    public static void syncFile(DBRProgressMonitor monitor, IFile localFile) {
        // Sync file with contents
        try {
            localFile.refreshLocal(IFile.DEPTH_ZERO, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Could not synchronize file '" + localFile + "' with contents", e);
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
        InputStream fileStream = new FileInputStream(file);
        try {
            UnicodeReader unicodeReader = new UnicodeReader(fileStream, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
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
        } finally {
            ContentUtils.close(fileStream);
        }
    }

    public static MimeType getMimeType(String contentType)
    {
        MimeType mimeType = null;
        if (contentType != null) {
            try {
                mimeType = new MimeType(contentType);
            } catch (MimeTypeParseException e) {
                log.error("Invalid content MIME type", e);
            }
        }
        return mimeType;
    }

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
        if (!CommonUtils.isEmpty(containers)) {
            IContainer container = containers[0];
            file = container.getFile(path.removeFirstSegments(path.segmentCount() - 1));
        }
        return file;
    }

    public static IPath convertPathToWorkspacePath(IPath path)
    {
        IFile wFile = convertPathToWorkspaceFile(path);
        return wFile == null ? null : wFile.getFullPath();
    }

}
