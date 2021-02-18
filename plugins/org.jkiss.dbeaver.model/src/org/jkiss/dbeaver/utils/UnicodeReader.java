/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class UnicodeReader extends Reader {

    private static final int BOM_SIZE = 4;
    private final InputStreamReader reader;

    /**
     * Construct UnicodeReader
     *
     * @param in              Input stream.
     * @param defaultCharset Default encoding to be used if BOM is not found,
     *                        or <code>null</code> to use system default encoding.
     * @throws IOException If an I/O error occurs.
     */
    public UnicodeReader(InputStream in, Charset defaultCharset) throws IOException
    {
        byte bom[] = new byte[BOM_SIZE];
        Charset encoding;
        int unread;
        PushbackInputStream pushbackStream = new PushbackInputStream(in, BOM_SIZE);
        int n = pushbackStream.read(bom, 0, bom.length);

        // Read ahead four bytes and check for BOM marks.
        if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            encoding = StandardCharsets.UTF_8;
            unread = n - 3;
        } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
            encoding = StandardCharsets.UTF_16BE;
            unread = n - 2;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
            encoding = StandardCharsets.UTF_16LE;
            unread = n - 2;
        } else if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
            encoding = Charset.forName("UTF-32BE");
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
            encoding = Charset.forName("UTF-32LE");
            unread = n - 4;
        } else {
            encoding = defaultCharset;
            unread = n;
        }

        // Unread bytes if necessary and skip BOM marks.
        if (unread > 0) {
            pushbackStream.unread(bom, (n - unread), unread);
        } else if (unread < -1) {
            pushbackStream.unread(bom, 0, 0);
        }

        // Use given encoding.
        if (encoding == null) {
            reader = new InputStreamReader(pushbackStream);
        } else {
            reader = new InputStreamReader(pushbackStream, encoding);
        }
    }

    public String getEncoding()
    {
        return reader.getEncoding();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException
    {
        reader.close();
    }
}
