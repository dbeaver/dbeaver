/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
    
    public static final String SYSTEM_GENERATED_PREFIX = "__SYS_";
    
    public static final int PACKAGE_TYPE_SPEC = 6;
    public static final int PACKAGE_TYPE_BODY = 7;

    /* TODO: remove later if not used anymore 
    public static final int TYPE_BIGINT = Types.BIGINT;
    public static final int TYPE_BINARY = Types.BINARY;
    public static final int TYPE_BIT = Types.BIT;
    public static final int TYPE_BLOB = 30;
    public static final int TYPE_BYTE = 20001;
    public static final int TYPE_CHAR = Types.CHAR;
    public static final int TYPE_CLOB = 40;
    public static final int TYPE_DATE = 9;
    public static final int TYPE_DOUBLE = Types.DOUBLE;
    public static final int TYPE_ECHAR = 60;
    public static final int TYPE_EVARCHAR = 61;
    public static final int TYPE_FLOAT = Types.FLOAT;
    
    public static final int TYPE_INTEGER = Types.INTEGER;
    public static final int TYPE_NCHAR = -8;
    public static final int TYPE_NIBBLE = 20002;
    public static final int TYPE_NUMBER = 10002;
    public static final int TYPE_NUMERIC = Types.NUMERIC;
    public static final int TYPE_NVARCHAR = -9;
    public static final int TYPE_REAL = Types.REAL;
    public static final int TYPE_SMALLINT = Types.SMALLINT;
    public static final int TYPE_TIMESTAMP = 3010;
    public static final int TYPE_VARBIT = -100;
    public static final int TYPE_VARBYTE = 20003;
    public static final int TYPE_VARCHAR = 12;

    public static final String TYPE_NAME_CHAR = "CHAR";
    public static final String TYPE_NAME_VARCHAR = "VARCHAR";
    public static final String TYPE_NAME_NCHAR = "NCHAR";
    public static final String TYPE_NAME_NVARCHAR = "NVARCHAR";
    
    
    public static final String TYPE_NAME_DATE = "DATE";
    
    public static final String TYPE_NAME_BLOB = "BLOB";
    public static final String TYPE_NAME_CLOB = "CLOB";
    
    public static final String TYPE_NAME_BYTE = "BYTE"; // for reflection
    public static final String TYPE_NAME_VARBYTE = "VARBYTE";
    public static final String TYPE_NAME_BIT = "BIT";
    public static final String TYPE_NAME_VARBIT = "VARBIT";
    public static final String TYPE_NAME_NIBBLE = "VARBYTE";
    public static final String TYPE_NAME_BINARY = "BINARY";
    public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";
        
    public static final String TYPE_NAME_BIT = "BIT";
    public static final String TYPE_NAME_VARBIT = "VARBIT";
    */
    public static final int TYPE_GEOMETRY = 10003;
    public static final String TYPE_NAME_GEOMETRY = "GEOMETRY";
    public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";
    public static final String TYPE_NAME_DATE = "DATE";
    
    public static final String OBJ_TYPE_MATERIALIZED_VIEW = "MATERIALIZED VIEW";
    public static final String OBJ_TYPE_TYPESET = "TYPESET";
    
    public static final String NO_DBMS_METADATA = "-- [WARNING] Without DBMS_METADATA package, " 
            + "the generated DDL may not be correct." + NEW_LINE;
    
    /*
     * Preference page
     */
    public static final String PREF_EXPLAIN_PLAN_TYPE = "altibase.explain.plan.type";
    public static final String PREF_DBMS_OUTPUT = "altibase.dbms.output";
    public static final String PREF_PLAN_PREFIX = "altibase.plan.prefix";
    
    public enum ExplainPlan {
        ONLY("EXPLAIN PLAN = ONLY", (byte) 2),
        ON("EXPLAIN PLAN = ON", (byte) 1);
        
        private String title;
        private byte value;
        
        ExplainPlan(String title, byte value) {
            this.title = title;
            this.value = value;
        }
        
        public String getTitle() {
            return this.title;
        }
        
        public byte getArgValue() {
            return this.value;
        }
        
        /**
         * Get EXPLAIN_PLAN enum object by index
         */
        public static ExplainPlan getByIndex(int idx) throws ArrayIndexOutOfBoundsException {
            for (ExplainPlan expPlan : ExplainPlan.values()) {
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
    public static final int EC_DBMS_METADATA_NOT_FOUND = 0xF1B3D;

    /*
     * DBMS output
     */
    //public static final String CLASS_NAME_4_CONNECTION = "Altibase.jdbc.driver.AltibaseConnection";
    // e.g. Altibase.jdbc.driver.AltibaseMessageCallback, Altibase7_1.jdbc.driver.AltibaseMessageCallback
    public static final String CLASS_NAME_4_CONNECTION_POSTFIX = ".jdbc.driver.AltibaseConnection";
    public static final String CLASS_NAME_4_MESSAGE_CALLBACK_POSTFIX = ".jdbc.driver.AltibaseMessageCallback";
    public static final String METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK = "registerMessageCallback"; 
}
