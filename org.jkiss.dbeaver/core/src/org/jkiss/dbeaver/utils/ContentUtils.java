/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.Closeable;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;

    static Log log = LogFactory.getLog(ContentUtils.class);

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
}
