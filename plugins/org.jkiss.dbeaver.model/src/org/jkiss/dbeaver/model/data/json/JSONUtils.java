/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.data.json;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.utils.CommonUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * JSON utils
 */
public class JSONUtils {

    private static final Log log = Log.getLog(JSONUtils.class);

    private static SimpleDateFormat dateFormat;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat(DBConstants.DEFAULT_ISO_TIMESTAMP_FORMAT);
        dateFormat.setTimeZone(tz);
    }

    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public static Date parseDate(String str) {
        if (CommonUtils.isEmpty(str)) {
            return null;
        }
        try {
            return dateFormat.parse(str);
        } catch (ParseException e) {
            log.error("Error parsing date");
            return new Date(0l);
        }
    }

    public static String formatISODate(Date date) {
        return "ISODate(\"" + formatDate(date) + "\")";  //$NON-NLS-1$//$NON-NLS-2$
    }

    public static String escapeJsonString(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '"':
                case '\\':
                case '/':
                    result.append("\\").append(c);
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }
}
