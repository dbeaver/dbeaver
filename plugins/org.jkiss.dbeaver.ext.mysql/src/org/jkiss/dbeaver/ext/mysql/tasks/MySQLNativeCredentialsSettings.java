package org.jkiss.dbeaver.ext.mysql.tasks;

public interface MySQLNativeCredentialsSettings {
    String PREFERENCE_NAME = "MySQL.overrideNativeCredentials";

    boolean isOverrideCredentials();

    void setOverrideCredentials(boolean value);
}
