package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public abstract class MySQLNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
        extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {

    protected List<String> getMySQLToolCommandLine(AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> handler, SETTINGS settings, PROCESS_ARG arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        handler.fillProcessParameters(settings, arg, cmd);

        String toolUserName = settings.getToolUserName();
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserName();
        }

        String toolUserPassword = settings.getToolUserPassword();
        if (CommonUtils.isEmpty(toolUserPassword)) {
            toolUserPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        }

        /*
         * Let's assume that if username is not set, then
         * native tool must search for credentials on its own.
         *
         * Native tools like mysqldump doesn't read password set in
         * MYSQL_PWD (also it's deprecated since MySQL 8.0) and prefer
         * to grab credentials from my.cnf (that is located somewhere in the system),
         * so we'll generate our own my.cnf with required credentials (#5350)
         */
        if (!CommonUtils.isEmpty(toolUserName)) {
            File dir = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "mysql-native-handler");
            File cnf = new File(dir, "my.cnf");

            try (Writer writer = new FileWriter(cnf)) {
                writer.write("[client]");
                writer.write("\nuser=" + (CommonUtils.isEmpty(toolUserName) ? "" : toolUserName));
                writer.write("\npassword=" + (CommonUtils.isEmpty(toolUserPassword) ? "" : toolUserPassword));
            }

            cmd.add(1, "--defaults-file=" + cnf.getPath());
        }

        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }

        return cmd;
    }

}
