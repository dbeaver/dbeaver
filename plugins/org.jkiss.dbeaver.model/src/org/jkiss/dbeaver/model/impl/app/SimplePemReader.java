/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * A generic PEM reader, based on the format outlined in RFC 1421
 */
public class SimplePemReader extends BufferedReader {

    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";
    private static final Log log = Log.getLog(SimplePemReader.class);

    public SimplePemReader(@NotNull Reader reader) {
        super(reader);
    }

    @NotNull
    public byte[] readPemObject() throws IOException {
        String line = readLine();

        while (line != null && !line.startsWith(BEGIN)) {
            line = readLine();
        }

        if (line != null) {
            line = line.substring(BEGIN.length()).trim();
            int index = line.indexOf('-');

            if (index > 0 && line.endsWith("-----") && (line.length() - index) == 5) {
                String type = line.substring(0, index);

                return loadObject(type);
            }
        }

        throw new IOException("No content in PEM file");
    }

    private byte[] loadObject(String type) throws IOException {
        String line;
        String endMarker = END + type + "-----";
        StringBuilder buf = new StringBuilder();
        List<String> headers = new ArrayList<>();

        while ((line = readLine()) != null) {
            int index = line.indexOf(':');
            if (index >= 0) {
                // Header - skip it
                continue;
            }
            if (line.indexOf(endMarker) == 0) {
                break;
            }

            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        return Base64.getDecoder().decode(buf.toString());
    }

}
