/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ProgressStreamReaderTest extends DBeaverUnitTest {

    @Test
    public void readSingleByteReportsOneByte() throws IOException {
        ProgressStreamReader reader = reader('A');

        Assertions.assertEquals('A', reader.read());

        Mockito.verify(monitor).worked(1);
    }

    @Test
    public void readSingleByteAtEofDoesNotReportProgress() throws IOException {
        ProgressStreamReader reader = reader();

        Assertions.assertEquals(-1, reader.read());

        Mockito.verify(monitor, Mockito.never()).worked(Mockito.anyInt());
    }

    @Test
    public void readByteArrayReportsBytesRead() throws IOException {
        ProgressStreamReader reader = reader(1, 2, 3);
        byte[] buffer = new byte[3];

        Assertions.assertEquals(3, reader.read(buffer));

        Mockito.verify(monitor).worked(3);
    }

    @Test
    public void readByteArrayAtEofDoesNotReportProgress() throws IOException {
        ProgressStreamReader reader = reader();

        Assertions.assertEquals(-1, reader.read(new byte[3]));

        Mockito.verify(monitor, Mockito.never()).worked(Mockito.anyInt());
    }

    @Test
    public void readByteArrayWithOffsetReportsBytesRead() throws IOException {
        ProgressStreamReader reader = reader(1, 2, 3);
        byte[] buffer = new byte[4];

        Assertions.assertEquals(2, reader.read(buffer, 1, 2));

        Mockito.verify(monitor).worked(2);
    }

    @Test
    public void skipZeroDoesNotReportProgress() throws IOException {
        ProgressStreamReader reader = reader(1, 2, 3);

        Assertions.assertEquals(0, reader.skip(0));

        Mockito.verify(monitor, Mockito.never()).worked(Mockito.anyInt());
    }

    @Test
    public void skipReportsBytesSkipped() throws IOException {
        ProgressStreamReader reader = reader(1, 2, 3);

        Assertions.assertEquals(2, reader.skip(2));

        Mockito.verify(monitor).worked(2);
    }

    private ProgressStreamReader reader(int... bytes) {
        return new ProgressStreamReader(
            monitor,
            "test",
            new ByteArrayInputStream(toByteArray(bytes)),
            bytes.length);
    }

    private static byte[] toByteArray(int... bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) bytes[i];
        }
        return result;
    }
}
