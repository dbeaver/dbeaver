/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.encoders;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Binary stream reader
 */
public class BinaryStreamReader extends Reader {

    private InputStream stream;
    private byte[] tmpBuf;

    public BinaryStreamReader(InputStream stream)
    {
        this.stream = stream;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        if (tmpBuf == null || tmpBuf.length < len) {
            tmpBuf = new byte[len];
        }
        int count = stream.read(tmpBuf, 0, len);
        for (int i = 0; i < count; i++) {
            cbuf[off + i] = (char)tmpBuf[i];
        }
        return count;
    }

    @Override
    public void close() throws IOException
    {
        tmpBuf = null;
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }
}
