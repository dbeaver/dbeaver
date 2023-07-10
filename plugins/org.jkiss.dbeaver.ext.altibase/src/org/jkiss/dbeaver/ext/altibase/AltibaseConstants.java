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

import org.jkiss.utils.StandardConstants;

public class AltibaseConstants {
    
    public static final String NEW_LINE = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);
    public static final String SPACE4 = "    ";
    public static final String PSM_POSTFIX = ";" + AltibaseConstants.NEW_LINE + "/";

    public static final String PUBLIC_USER = "PUBLIC";
    
    public static final int PACKAGE_SPEC = 6;
    public static final int PACKAGE_BODY = 7;

    public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";
    public static final String PROP_OBJECT_BODY_DEFINITION = "extendedDefinitionText";

    public static final int TYPE_BIGINT = -5;
    public static final int TYPE_BINARY = -2;
    public static final int TYPE_BIT = -7;
    public static final int TYPE_BLOB = 30;
    public static final int TYPE_BYTE = 20001;
    public static final int TYPE_CHAR = 1;
    public static final int TYPE_CLOB = 40;
    public static final int TYPE_DATE = 9;
    public static final int TYPE_DOUBLE = 8;
    public static final int TYPE_ECHAR = 60;
    public static final int TYPE_EVARCHAR = 61;
    public static final int TYPE_FLOAT = 6;
    public static final int TYPE_GEOMETRY = 10003;
    public static final int TYPE_INTEGER = 4;
    public static final int TYPE_NCHAR = -8;
    public static final int TYPE_NIBBLE = 20002;
    public static final int TYPE_NUMBER = 10002;
    public static final int TYPE_NUMERIC = 2;
    public static final int TYPE_NVARCHAR = -9;
    public static final int TYPE_REAL = 7;
    public static final int TYPE_SMALLINT = 5;
    public static final int TYPE_VARBIT = -100;
    public static final int TYPE_VARBYTE = 20003;
    public static final int TYPE_VARCHAR = 12;

    public static final String TYPE_NAME_GEOMETRY = "GEOMETRY";
    public static final String TYPE_NAME_CLOB = "CLOB";
    public static final String TYPE_NAME_DATE = "DATE";
    public static final String TYPE_NAME_BYTE = "BYTE"; // for reflection
    
    public static final String OBJ_TYPE_MATERIALIZED_VIEW = "MATERIALIZED VIEW";
    public static final String OBJ_TYPE_TYPESET = "TYPESET";
    
    public static final String NO_DBMS_METADATA = 
    "-- [WARNING] Without DBMS_METADATA package, the generated DDL may not be correct." + NEW_LINE;
    

    /*
     * Preference page
     */
    public static final String PREF_EXPLAIN_PLAN_TYPE = "altibase.explain.plan.type";
    
    public enum EXPLAIN_PLAN {
        ONLY ("EXPLAIN PLAN = ONLY", (byte) 2),
        ON ("EXPLAIN PLAN = ON", (byte) 1);
        
        private String title;
        private byte value;
        
        EXPLAIN_PLAN(String title, byte value) {
            this.title = title;
            this.value = value;
        }
        
        public String getTitle() {
            return this.title;
        }
        
        public byte getArgValue() {
            return this.value;
        }
        
        public static EXPLAIN_PLAN getByIndex(int idx) throws ArrayIndexOutOfBoundsException {
            for(EXPLAIN_PLAN expPlan:EXPLAIN_PLAN.values()) {
                if (expPlan.ordinal() == idx) {
                    return expPlan;
                }
            }
            
            throw new ArrayIndexOutOfBoundsException("No such index value in EXPLAIN_PLAN: " + idx);
        }
    }
    
    /*
     * Password grace time
     */
    public static final String SQL_WARNING_TITILE = "SQL Warning";
    public static final String PASSWORD_WILL_EXPIRE_WARN_DESCRIPTION = "Change the password or contact the DBA.";
    
    public static final int EC_PASSWORD_WILL_EXPIRE = 0x51A79;
}
