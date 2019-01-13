/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;

public class ProgressStreamReader extends InputStream {

    static final int BUFFER_SIZE = 10000;

    private final DBRProgressMonitor monitor;
    private final InputStream original;
    private final long streamLength;
    private long totalRead;

    public ProgressStreamReader(DBRProgressMonitor monitor, String task, InputStream original, long streamLength)
    {
        this.monitor = monitor;
        this.original = original;
        this.streamLength = streamLength;
        this.totalRead = 0;

        monitor.beginTask(task, (int)streamLength);
    }

    @Override
    public int read() throws IOException
    {
        int res = original.read();
        showProgress(res);
        return res;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        int res = original.read(b);
        showProgress(res);
        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int res = original.read(b, off, len);
        showProgress(res);
        return res;
    }

    @Override
    public long skip(long n) throws IOException
    {
        long res = original.skip(n);
        showProgress(res);
        return res;
    }

    @Override
    public int available() throws IOException
    {
        return original.available();
    }

    @Override
    public void close() throws IOException
    {
        monitor.done();
        original.close();
    }

    private void showProgress(long length)
    {
        totalRead += length;
        monitor.worked((int)length);
    }
}
