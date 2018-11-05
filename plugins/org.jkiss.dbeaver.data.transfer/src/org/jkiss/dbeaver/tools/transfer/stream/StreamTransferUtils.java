/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterCSV;

import java.util.Map;

/**
 * Stream transfer utils
 */
public class StreamTransferUtils {

    private static final Log log = Log.getLog(StreamTransferUtils.class);

    private static final char DEF_DELIMITER = ',';

    public static String getDelimiterString(Map<Object, Object> properties, String propName) {
        String delimString = String.valueOf(properties.get(propName));
        if (delimString == null || delimString.isEmpty()) {
            delimString = String.valueOf(DEF_DELIMITER);
        } else {
            delimString = delimString
                    .replace("\\t", "\t")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r");
        }
        return delimString;
    }
}
