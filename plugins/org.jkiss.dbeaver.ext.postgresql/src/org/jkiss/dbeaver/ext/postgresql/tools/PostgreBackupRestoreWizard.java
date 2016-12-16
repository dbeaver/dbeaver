/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.ui.IExportWizard;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreServerHome;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractImportExportWizard;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

abstract class PostgreBackupRestoreWizard<PROCESS_ARG extends PostgreDatabaseBackupRestoreInfo> extends AbstractImportExportWizard<PROCESS_ARG> implements IExportWizard {

    public enum ExportFormat {
        PLAIN("p", "Plain"),
        CUSTOM("c", "Custom"),
        DIRECTORY("d", "Directory"),
        TAR("t", "Tar");

        private final String id;
        private String title;

        ExportFormat(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    ExportFormat format = ExportFormat.CUSTOM;

    public PostgreBackupRestoreWizard(Collection<DBSObject> objects, String title) {
        super(objects, title);
    }

    @Override
    public void fillProcessParameters(List<String> cmd, PROCESS_ARG arg) throws IOException
    {
        File dumpBinary = RuntimeUtils.getHomeBinary(getClientHome(), PostgreConstants.BIN_FOLDER,
            isExportWizard() ? "pg_dump" : "pg_restore"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    protected void setupProcessParameters(ProcessBuilder process) {
        if (!CommonUtils.isEmpty(getToolUserPassword())) {
            process.environment().put("PGPASSWORD", getToolUserPassword());
        }
    }

    @Override
    public PostgreServerHome findServerHome(String clientHomeId)
    {
        return PostgreDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine(PROCESS_ARG arg) throws IOException
    {
        return PostgreToolScript.getPostgreToolCommandLine(this, arg);
    }

    @Override
    public boolean isVerbose()
    {
        return true;
    }

}
