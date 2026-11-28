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
package org.jkiss.dbeaver.ext.firebird.model;

public class FireBirdConstants  {

    public static final String TYPE_FIELD_TYPE = "RDB$FIELD_TYPE";
    public static final String TYPE_FIELD_SUB_TYPE = "RDB$FIELD_SUB_TYPE";
    public static final String TYPE_CHARACTER_SET_NAME = "RDB$CHARACTER_SET_NAME";

    /** RDB$USER_TYPE / RDB$OBJECT_TYPE of a role in RDB$USER_PRIVILEGES */
    public static final int OBJECT_TYPE_ROLE = 13;
    /** RDB$FIELD_NAME of a role membership granted as default role (GRANT DEFAULT, Firebird 4+) */
    public static final String DEFAULT_ROLE_FLAG = "D";
    /** Grantee that stands for all users */
    public static final String GRANTEE_PUBLIC = "PUBLIC";

    /**
     * System privileges of a role (Firebird 4+), indexed by their id in Firebird's src/jrd/SystemPrivileges.h.
     * Id 0 is the unused NULL_PRIVILEGE. In RDB$ROLES.RDB$SYSTEM_PRIVILEGES privilege p is bit (p % 8) of byte (p / 8).
     */
    public static final String[] SYSTEM_PRIVILEGES = {
        null,
        "USER_MANAGEMENT",
        "READ_RAW_PAGES",
        "CREATE_USER_TYPES",
        "USE_NBACKUP_UTILITY",
        "CHANGE_SHUTDOWN_MODE",
        "TRACE_ANY_ATTACHMENT",
        "MONITOR_ANY_ATTACHMENT",
        "ACCESS_SHUTDOWN_DATABASE",
        "CREATE_DATABASE",
        "DROP_DATABASE",
        "USE_GBAK_UTILITY",
        "USE_GSTAT_UTILITY",
        "USE_GFIX_UTILITY",
        "IGNORE_DB_TRIGGERS",
        "CHANGE_HEADER_SETTINGS",
        "SELECT_ANY_OBJECT_IN_DATABASE",
        "ACCESS_ANY_OBJECT_IN_DATABASE",
        "MODIFY_ANY_OBJECT_IN_DATABASE",
        "CHANGE_MAPPING_RULES",
        "USE_GRANTED_BY_CLAUSE",
        "GRANT_REVOKE_ON_ANY_OBJECT",
        "GRANT_REVOKE_ANY_DDL_RIGHT",
        "CREATE_PRIVILEGED_ROLES",
        "GET_DBCRYPT_INFO",
        "MODIFY_EXT_CONN_POOL",
        "REPLICATE_INTO_DATABASE",
        "PROFILE_ANY_ATTACHMENT"
    };

    public static final String CHARSET_OCTETS = "OCTETS";
    public static final String CHARSET_BINARY = "BINARY";
}