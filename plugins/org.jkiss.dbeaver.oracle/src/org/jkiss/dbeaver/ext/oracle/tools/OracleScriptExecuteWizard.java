/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.tools;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class OracleScriptExecuteWizard extends AbstractScriptExecuteWizard<OracleDataSource> {

    private OracleScriptExecuteWizardPageSettings mainPage;

    public OracleScriptExecuteWizard(OracleDataSource oracleSchema)
    {
        super(oracleSchema, OracleMessages.tools_script_execute_wizard_page_name);
        this.mainPage = new OracleScriptExecuteWizardPageSettings(this);
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd) throws IOException
    {
        String sqlPlusExec = RuntimeUtils.getNativeBinaryName("sqlplus"); //$NON-NLS-1$
        File sqlPlusBinary = new File(getClientHome().getHomePath(), "bin/" + sqlPlusExec); //$NON-NLS-1$
        if (!sqlPlusBinary.exists()) {
            sqlPlusBinary = new File(getClientHome().getHomePath(), sqlPlusExec);
        }
        if (!sqlPlusBinary.exists()) {
            throw new IOException(NLS.bind(OracleMessages.tools_script_execute_wizard_error_sqlplus_not_found, getClientHome().getDisplayName()));
        }
        String dumpPath = sqlPlusBinary.getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    public OracleHomeDescriptor findServerHome(String clientHomeId)
    {
        return OCIUtils.getOraHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine() throws IOException
    {
        List<String> cmd = new ArrayList<String>();
        fillProcessParameters(cmd);
        DBPConnectionInfo conInfo = getConnectionInfo();
        String url;
        if ("TNS".equals(conInfo.getProperties().get(OracleConstants.PROP_CONNECTION_TYPE))) { //$NON-NLS-1$
            url = conInfo.getServerName();
        }
        else {
            String port = conInfo.getHostPort();
            url = "//" + conInfo.getHostName() + (port != null ? ":" + port : "") + "/" + conInfo.getDatabaseName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        cmd.add(conInfo.getUserName() + "/" + conInfo.getUserPassword() + "@" + url); //$NON-NLS-1$ //$NON-NLS-2$
/*

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        cmd.add("-q");

        cmd.add(toolWizard.getDatabaseObject().getName());
*/
        return cmd;
    }
}
