/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;

    static void copyStreams(
        InputStream inputStream,
        long contentLength,
        OutputStream outputStream,
        IProgressMonitor monitor)
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

    static void copyStreams(
        Reader reader,
        long contentLength,
        Writer writer,
        IProgressMonitor monitor)
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
}
