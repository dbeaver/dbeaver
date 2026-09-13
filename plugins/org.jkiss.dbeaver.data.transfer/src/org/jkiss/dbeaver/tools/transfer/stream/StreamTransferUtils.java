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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * Stream transfer serialize
 */
public class StreamTransferUtils {

    private static final Log log = Log.getLog(StreamTransferUtils.class);

    private static final String DEF_DELIMITER = ",";

    public static String getDelimiterString(Map<String, Object> properties, String propName) {
        String delimString = CommonUtils.toString(properties.get(propName), null);
        if (CommonUtils.isEmpty(delimString)) {
            return DEF_DELIMITER;
        } else {
            return delimString
                    .replace("\\t", "\t")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r");
        }
    }

    public static void writeBinaryData(
        @NotNull DBDContentStorage contentStorage,
        @NotNull StreamConsumerSettings.LobEncoding encoding,
        @Nullable DBPDataSource dataSource,
        @NotNull Writer writer
    ) throws IOException {
        try (InputStream stream = contentStorage.getContentStream()) {
            switch (encoding) {
                case BASE64:
                    Base64.encode(stream, contentStorage.getContentLength(), writer);
                    break;
                case HEX:
                    writer.write("0x"); //$NON-NLS-1$
                    byte[] buffer = new byte[5000];
                    for (;;) {
                        int count = stream.read(buffer);
                        if (count <= 0) {
                            break;
                        }
                        GeneralUtils.writeBytesAsHex(writer, buffer, 0, count);
                    }
                    break;
                case NATIVE:
                    if (dataSource != null) {
                        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream((int) contentStorage.getContentLength());
                        IOUtils.copyStream(stream, bufferStream);

                        byte[] bytes = bufferStream.toByteArray();
                        DBDBinaryFormatter formatter = dataSource.getSQLDialect().getNativeBinaryFormatter();
                        writer.write(formatter.toString(bytes, 0, bytes.length));
                    }
                    break;
                case BINARY:
                default:
                    Reader reader = new InputStreamReader(stream, contentStorage.getCharset());
                    char[] readBuffer = new char[1000];
                    for (;;) {
                        int count = reader.read(readBuffer);
                        if (count <= 0) {
                            break;
                        }
                        String content = new String(readBuffer, 0, count);
                        writer.write(JSONUtils.escapeJsonString(content));
                    }
                    break;
            }
        }
    }
}
