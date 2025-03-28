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
