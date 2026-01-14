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

package org.jkiss.dbeaver.ext.iotdb;

public class IoTDBPrivilegeInfo {

    public enum Kind {
        GLOBAL,
        DATABASE,
        SERIES
    }

    public static String[] treeGlobalPrivileges = {
        "EXTEND_TEMPLATE",
        "MAINTAIN",
        "MANAGE_DATABASE",
        "MANAGE_ROLE",
        "MANAGE_USER",
        "USE_CQ",
        "USE_MODEL",
        "USE_PIPE",
        "USE_TRIGGER",
        "USE_UDF",
    };

    public static String[] treeSeriesPrivileges = {"READ_DATA", "WRITE_DATA", "READ_SCHEMA", "WRITE_SCHEMA"};

    public static String[] tableGlobalPrivileges = {"MAINTAIN", "MANAGE_ROLE", "MANAGE_USER"};

    public static String[] tableDatabasePrivileges = {"ALTER", "CREATE", "DELETE", "DROP", "INSERT", "SELECT"};
}
