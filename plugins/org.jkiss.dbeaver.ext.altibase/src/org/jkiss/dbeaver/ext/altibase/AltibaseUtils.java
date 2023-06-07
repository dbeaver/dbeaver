/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.altibase;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.StandardConstants;

/**
 * Altibase utils
 */
public class AltibaseUtils {

    private static final Log log = Log.getLog(AltibaseUtils.class);

    public static final String NEW_LINE = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    public static boolean isEmpty(String strVal) {
        if (strVal != null) {
            return (strVal.length() < 1);
        } else {
            return true;
        }
    }

    /*
     * DBMS_METADATA: Object type name
     * In case of space in the name, DBMS_METADATA requires replace space to underscore.
     */
    public static String getDmbsMetaDataObjTypeName(String objTypeName) {
        if (isEmpty(objTypeName)) {
            return "UNKNOWN_OBJECT_TYPE";
        }

        return objTypeName.replaceAll(" ", "_");
    }

    public static String getQuotedName(String schemaName, String objName) {
        StringBuilder quotedName = new StringBuilder();

        if (isEmpty(schemaName) == false) {
            quotedName.append("\"").append(schemaName).append("\".");
        }

        if (isEmpty(objName) == false) {
            quotedName.append("\"").append(objName).append("\"");
        }

        return quotedName.toString();
    }

}
