package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OracleScriptExecuteHandler extends AbstractNativeToolHandler<OracleScriptExecuteSettings, DBSObject, OracleDataSource> {

    @Override
    public Collection<OracleDataSource> getRunInfo(OracleScriptExecuteSettings settings) {
        return Collections.singletonList((OracleDataSource) settings.getDataSourceContainer().getDataSource());
    }

    @Override
    protected OracleScriptExecuteSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        OracleScriptExecuteSettings settings = new OracleScriptExecuteSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean needsModelRefresh() {
        return false;
    }

    @Override
    public void fillProcessParameters(OracleScriptExecuteSettings settings, OracleDataSource arg, List<String> cmd) throws IOException {
        String sqlPlusExec = RuntimeUtils.getNativeBinaryName("sqlplus"); //$NON-NLS-1$
        File sqlPlusBinary = new File(settings.getClientHome().getPath(), "bin/" + sqlPlusExec); //$NON-NLS-1$
        if (!sqlPlusBinary.exists()) {
            sqlPlusBinary = new File(settings.getClientHome().getPath(), sqlPlusExec);
        }
        if (!sqlPlusBinary.exists()) {
            throw new IOException("SQL*Plus binary not found in '" + settings.getClientHome().getPath().getAbsolutePath());
        }
        String dumpPath = sqlPlusBinary.getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    protected List<String> getCommandLine(OracleScriptExecuteSettings settings, OracleDataSource arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);
        DBPConnectionConfiguration conInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        String url;
        if ("TNS".equals(conInfo.getProviderProperty(OracleConstants.PROP_CONNECTION_TYPE))) { //$NON-NLS-1$
            url = conInfo.getServerName() != null ? conInfo.getServerName() : conInfo.getDatabaseName();
        }
        else {
            boolean isSID = OracleConnectionType.SID.name().equals(conInfo.getProviderProperty(OracleConstants.PROP_SID_SERVICE));
            String port = conInfo.getHostPort();
            if (isSID) {
                url = "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(Host=" + conInfo.getHostName() + ")(Port=" + port + "))(CONNECT_DATA=(SID=" + conInfo.getDatabaseName() + ")))";
            } else {
                url = "//" + conInfo.getHostName() + (port != null ? ":" + port : "") + "/" + conInfo.getDatabaseName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
        final String role = conInfo.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        if (role != null) {
            url += (" AS " + role);
        }
        cmd.add(conInfo.getUserName() + "/" + conInfo.getUserPassword() + "@" + url); //$NON-NLS-1$ //$NON-NLS-2$
/*

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        cmd.add("-q");

        cmd.add(toolWizard.getDatabaseObjects().getName());
*/
        return cmd;
    }

    @Override
    protected boolean isLogInputStream() {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, OracleScriptExecuteSettings settings, OracleDataSource arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        final File inputFile = new File(settings.getInputFile());
        if (!inputFile.exists()) {
            throw new IOException("File '" + inputFile.getAbsolutePath() + "' doesn't exist");
        }
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        new BinaryFileTransformerJob(monitor, task, inputFile, process.getOutputStream(), log).start();
    }

}
