package org.jkiss.dbeaver.ext.oceanbase.model.auth;

import java.util.Properties;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class OceanbaseAuthModelDatabaseNative extends AuthModelDatabaseNative {

    public static final String ID = "oceanbase_native";

    @Override
    public Object initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource,
            AuthModelDatabaseNativeCredentials credentials, DBPConnectionConfiguration configuration,
            @NotNull Properties connProperties) throws DBException {
        String userName = configuration.getUserName();
        if (!CommonUtils.isEmpty(userName)) {
            if (!userName.contains("@")) {
                userName += "@" + configuration.getServerName();
            }
        }

        credentials.setUserName(userName);
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProperties);
    }

}
