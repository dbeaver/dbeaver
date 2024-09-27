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

import org.jkiss.code.NotNull;
import org.jkiss.utils.StandardConstants;

public class AltibaseConstants {
    
    public static final boolean DEBUG = false;
    
    public static final String NEW_LINE = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    public static final String DBOBJ_INDEX = "INDEX";
    public static final String USER_PUBLIC = "PUBLIC";
    public static final String USER_SYSTEM_ = "SYSTEM_";
    public static final String USER_SYS = "SYS";
    
    public static boolean isSysUser(@NotNull String userName) {
        return (USER_SYS.equals(userName) || USER_SYSTEM_.equals(userName));
    }
    
    public static final String SYSTEM_GENERATED_PREFIX = "__SYS_";

    public static final int PACKAGE_TYPE_SPEC = 6;
    public static final int PACKAGE_TYPE_BODY = 7;
    
    public static final int TYPE_BYTE = 20001;
    public static final int TYPE_VARBYTE = 20003;
    public static final int TYPE_NIBBLE = 20002;
    
    public static final int TYPE_GEOMETRY = 10003;
    public static final String TYPE_NAME_GEOMETRY = "GEOMETRY";
    public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";
    public static final String TYPE_NAME_DATE = "DATE";

    public static final String OPERATION_MODIFY = "MODIFY";
    
    public static final String SRID_EQ = "SRID=";
    
    public static final String OBJ_TYPE_MATERIALIZED_VIEW = "MATERIALIZED VIEW";
    public static final String OBJ_TYPE_TYPESET = "TYPESET";
    
    public static final String NO_DBMS_METADATA = "-- [WARNING] Without DBMS_METADATA package, " 
            + "the generated DDL may not be correct." + NEW_LINE;
    
    public static final String NO_DDL_WITHOUT_DBMS_METADATA = 
            "-- [WARNING] DBMS_METADATA package is required to have DDL for %s." + NEW_LINE;
    
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
    
    public static final String SQL_STATE_TOO_LONG = "22026";
    
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
    // e.g. Altibase.jdbc.driver.AltibaseMessageCallback, Altibase7_1.jdbc.driver.AltibaseMessageCallback
    public static final String CLASS_NAME_4_CONNECTION_POSTFIX = ".jdbc.driver.AltibaseConnection";
    public static final String CLASS_NAME_4_MESSAGE_CALLBACK_POSTFIX = ".jdbc.driver.AltibaseMessageCallback";
    public static final String METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK = "registerMessageCallback";
    
    public static final String RESULT_YES_VALUE = "YES";
    public static final String RESULT_Y_VALUE = "Y";
    public static final String RESULT_T_VALUE = "T";
    public static final String RESULT_1_VALUE = "1";
    
    /*
     * PSM Type: SYS_PROCEDURES_ (OBJECT_TYPE)
     */
    public static final int PSM_TYPE_PROCEDURE  = 0;
    public static final int PSM_TYPE_FUNCTION   = 1;
    public static final int PSM_TYPE_TYPESET    = 3;

    /*
     * V$PROPERTY attributes
     */
    public static final long IDP_ATTR_RD_WRITABLE = 0x00000000;
    public static final long IDP_ATTR_RD_READONLY = 0x00000002;
    
    public static final String ALTER_REPL_PREFIX = "ALTER REPLICATION";
}