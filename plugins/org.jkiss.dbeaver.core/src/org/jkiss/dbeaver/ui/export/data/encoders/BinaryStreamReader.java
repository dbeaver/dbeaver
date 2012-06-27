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
