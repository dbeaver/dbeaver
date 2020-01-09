package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class PostgreNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
    extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {

    @Override
    protected void setupProcessParameters(SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
        String userPassword = settings.getToolUserPassword();
        if (CommonUtils.isEmpty(userPassword)) {
            userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        }
        if (!CommonUtils.isEmpty(userPassword)) {
            process.environment().put("PGPASSWORD", userPassword);
        }
    }

    @Override
    public void fillProcessParameters(SETTINGS settings, PROCESS_ARG process_arg, List<String> cmd) throws IOException {
        File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), PostgreConstants.BIN_FOLDER,
            this instanceof PostgreDatabaseBackupHandler ? "pg_dump" :
                this instanceof PostgreDatabaseRestoreHandler ? "pg_restore" : "psql"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);

        if (isVerbose()) {
            cmd.add("--verbose");
        }
        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }
        String toolUserName = settings.getToolUserName();
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = connectionInfo.getUserName();
        }
        cmd.add("--username=" + toolUserName);

        settings.addExtraCommandArgs(cmd);
    }

    public boolean isVerbose() {
        return false;
    }

    protected abstract boolean isExportWizard();

}
