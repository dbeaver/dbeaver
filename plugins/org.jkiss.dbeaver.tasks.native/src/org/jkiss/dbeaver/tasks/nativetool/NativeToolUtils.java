package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.utils.CommonUtils;

public abstract class NativeToolUtils {

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TIMESTAMP = "timestamp";
    public static final String VARIABLE_CONN_TYPE = "connectionType";

    public static boolean isSecureString(AbstractNativeToolSettings settings, String string) {
        String userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        String toolUserPassword = settings.getToolUserPassword();
        return !CommonUtils.isEmpty(toolUserPassword) && string.endsWith(toolUserPassword) ||
            !CommonUtils.isEmpty(userPassword) && string.endsWith(userPassword);
    }

}
