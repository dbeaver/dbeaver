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
package org.jkiss.dbeaver.ext.cubrid;

public class CubridConstants
{
    public static final String OWNER_NAME = "owner_name";
    public static final String IS_SYSTEM_CLASS = "is_system_class";
    public static final String TERM_FUNCTION = "FUNCTION";
    public static final String TERM_PROCEDURE = "PROCEDURE";
    public static final String AUTO_INCREMENT_VAL = "current_val";
    public static final String COLLATION = "collation";
    public static final String DEFAULT_COLLATION = "utf8_bin";
    public static final String REUSE_OID = "is_reuse_oid_class";
    public static final String NAME = "name";
    public static final String COMMENT = "comment";
    public static final String DBA = "dba";
    public static final String[] EVENT_OPTION = {"UPDATE", "UPDATE STATEMENT", "DELETE", "DELETE STATEMENT", "INSERT", "INSERT STATEMENT", "COMMIT", "ROLLBACK"};
    public static final String[] ACTION_TIME_OPTION = {"BEFORE", "AFTER", "DEFFERED"};
    public static final String[] ACTION_TYPE_OPTION = {"OTHER STATEMENT", "REJECT", "INVALIDATE_TRANSACTION", "PRINT"};
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "30000";
    public static final String AUTO_INCREMENT = "auto_increment";
}
