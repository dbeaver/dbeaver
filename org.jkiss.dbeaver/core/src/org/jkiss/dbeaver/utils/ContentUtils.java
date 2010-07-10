/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;

    static Log log = LogFactory.getLog(ContentUtils.class);
    public static final String DEFAULT_FILE_CHARSET = "UTF-8";

    public static IFile createTempFile(DBRProgressMonitor monitor, String fileName)
    {
        return DBeaverCore.getInstance().makeTempFile(fileName, "data", monitor);
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

    public static File selectFileForSave(Shell parentShell)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
        fileDialog.setText("Save Content As");
        fileDialog.setOverwrite(true); 
        String fileName = fileDialog.open();
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File saveFile = new File(fileName);
        File saveDir = saveFile.getParentFile();
        if (!saveDir.exists()) {
            DBeaverUtils.showErrorDialog(parentShell, "Bad file name", "Directory '" + saveDir.getAbsolutePath() + "' does not exists");
            return null;
        }
        return saveFile;
    }

    public static File openFile(Shell parentShell)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
        String fileName = fileDialog.open();
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File loadFile = new File(fileName);
        if (!loadFile.exists()) {
            MessageBox aMessageBox = new MessageBox(parentShell, SWT.ICON_WARNING | SWT.OK);
            aMessageBox.setText("File doesn't exists");
            aMessageBox.setMessage("The file "+ loadFile.getAbsolutePath() + " doesn't exists.");
            aMessageBox.open();
            return null;
        }
        return loadFile;
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
    }

    public static void copyReaderToFile(DBRProgressMonitor monitor, Reader reader, long contentLength, String charset, IFile localFile)
        throws DBException, IOException
    {
        File file = localFile.getLocation().toFile();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            try {
                if (charset == null) {
                    charset = ContentUtils.DEFAULT_FILE_CHARSET;
                }
                Writer writer = new OutputStreamWriter(outputStream, charset);
                ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                writer.flush();
                try {
                    localFile.setCharset(charset, monitor.getNestedMonitor());
                }
                catch (CoreException e) {
                    log.warn(e);
                }
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            reader.close();
        }
    }
}
