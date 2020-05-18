package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class MySQLNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
    extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {

    @Override
    protected void setupProcessParameters(SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
        String userPassword = settings.getToolUserPassword();
        if (CommonUtils.isEmpty(userPassword)) {
            userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        }
        if (!CommonUtils.isEmpty(userPassword)) {
            process.environment().put(MySQLConstants.ENV_VARIABLE_MYSQL_PWD, userPassword);
        }/* else {
            // Empty password?
            process.environment().put(MySQLConstants.ENV_VARIABLE_MYSQL_PWD, "");
        }*/
    }

    protected List<String> getMySQLToolCommandLine(AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> handler, SETTINGS settings, PROCESS_ARG arg) throws IOException {
        java.util.List<String> cmd = new ArrayList<>();
        handler.fillProcessParameters(settings, arg, cmd);

        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }
        String toolUserName = settings.getToolUserName();
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserName();
        }
        if (!CommonUtils.isEmpty(toolUserName)) {
            cmd.add("-u");
            cmd.add(toolUserName);
        }
        // Password is passed in env variable (#1004)
//        if (!CommonUtils.isEmpty(toolWizard.getToolUserPassword())) {
//            cmd.add("--password=" + toolWizard.getToolUserPassword());
//        }

        return cmd;
    }

}
