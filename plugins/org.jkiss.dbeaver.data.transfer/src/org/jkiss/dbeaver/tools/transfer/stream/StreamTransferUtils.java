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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

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
}
