/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class StatOutputStream extends OutputStream {
    private final OutputStream stream;
    private long bytesWritten = 0;

    public StatOutputStream(OutputStream stream) {
        this.stream = stream;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public void write(int b) throws IOException {
        stream.write(b);
        bytesWritten++;
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        stream.write(b);
        bytesWritten += b.length;
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
        bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
