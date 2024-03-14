/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.utils.CommonUtils;

/**
 * Altibase utils
 */
public class AltibaseUtils {

    /**
     * DBMS_METADATA: Object type name
     * In case of space in the name, DBMS_METADATA requires replace space to underscore.
     */
    public static String getDmbsMetaDataObjTypeName(String objTypeName) {
        if (CommonUtils.isEmpty(objTypeName)) {
            return "UNKNOWN_OBJECT_TYPE";
        }

        return objTypeName.replaceAll(" ", "_");
    }
    
    /**
     * Get the first index of SQL that is not start with comment and has value
     * from SQL string array.
     */
    private static int getEffectiveSqlLineNumber(String[] script) {
        int i = 0;
        for (String line : script) {
            if ((line != null) && (line.trim().length() > 0) 
                    && !line.stripLeading().startsWith("--")) {
                return i;
            }
            i++;
        }
        
        return i;
    }
    
    /**
     * Remove View source comments generated by DBStrucUtils.java
     * e.g. -- SYS.SQ_VIEW1 source or empty line
     */
    public static String getEffectiveSql(String script) {
        StringBuilder ddl = new StringBuilder();
        String[] lines = script.split("\\R");

        int i = 0;
        int length = lines.length;
        int last;
        
        for (i = getEffectiveSqlLineNumber(lines); i < length; i++) {
            ddl.append(lines[i]).append(AltibaseConstants.NEW_LINE);
        }
        
        last = ddl.length() - AltibaseConstants.NEW_LINE.length();
        
        return ddl.substring(0, last);
    }
}
