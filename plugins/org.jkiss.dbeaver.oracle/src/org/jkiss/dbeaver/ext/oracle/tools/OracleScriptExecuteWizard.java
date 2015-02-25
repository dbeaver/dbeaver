/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.oracle.tools;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
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
        if ("TNS".equals(conInfo.getProperty(OracleConstants.PROP_CONNECTION_TYPE))) { //$NON-NLS-1$
            url = conInfo.getServerName();
        }
        else {
            boolean isSID = OracleConnectionType.SID.name().equals(conInfo.getProperty(OracleConstants.PROP_SID_SERVICE));
            String port = conInfo.getHostPort();
            if (isSID) {
                url = "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(Host=" + conInfo.getHostName() + ")(Port=" + port + "))(CONNECT_DATA=(SID=" + conInfo.getDatabaseName() + ")))";
            } else {
                url = "//" + conInfo.getHostName() + (port != null ? ":" + port : "") + "/" + conInfo.getDatabaseName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
        final Object role = conInfo.getProperty(OracleConstants.PROP_INTERNAL_LOGON);
        if (role != null) {
            url += (" AS " + role);
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
