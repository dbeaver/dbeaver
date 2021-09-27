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

/*
 * This package contains a slightly modified version of opencsv library
 * without unwanted functionality and dependencies, licensed under Apache 2.0.
 *
 * See https://search.maven.org/artifact/com.opencsv/opencsv/3.4/bundle
 * See http://opencsv.sf.net/
 */
package org.jkiss.utils.csv;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class was created for issue #106 (https://sourceforge.net/p/opencsv/bugs/106/) where
 * carriage returns were being removed.  This class allows the user to determine if they wish to keep or
 * remove them from the data being read.
 * <p/>
 * Created by scott on 2/19/15.
 */

public class LineReader {
    private final BufferedReader reader;
    private final boolean keepCarriageReturns;

    /**
     * LineReader constructor.
     *
     * @param reader              - Reader that data will be read from.
     * @param keepCarriageReturns - true if carriage returns should remain in the data, false to remove them.
     */
    public LineReader(BufferedReader reader, boolean keepCarriageReturns) {
        this.reader = reader;
        this.keepCarriageReturns = keepCarriageReturns;
    }

    /**
     * Reads the next line from the Reader.
     *
     * @return - Line read from reader.
     * @throws IOException - on error from BufferedReader
     */
    public String readLine() throws IOException {
        return keepCarriageReturns ? readUntilNewline() : reader.readLine();
    }

    private String readUntilNewline() throws IOException {
        StringBuilder sb = new StringBuilder(CSVParser.INITIAL_READ_SIZE);
        for (int c = reader.read(); c > -1 && c != '\n'; c = reader.read()) {
            sb.append((char) c);
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}
