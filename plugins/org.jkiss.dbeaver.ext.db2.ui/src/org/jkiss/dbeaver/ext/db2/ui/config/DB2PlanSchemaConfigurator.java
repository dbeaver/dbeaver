/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.db2.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2PlanConfig;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.ui.editors.DB2TablespaceChooser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * DB2 Explain Schema configurator
 */
public class DB2PlanSchemaConfigurator implements DBEObjectConfigurator<DB2PlanConfig> {
    protected static final Log log = Log.getLog(DB2PlanSchemaConfigurator.class);

    @Override
    public DB2PlanConfig configureObject(DBRProgressMonitor monitor, Object db2dataSource, DB2PlanConfig object) {
        DB2DataSource db2source = (DB2DataSource) db2dataSource;
        JDBCSession session = DBUtils.openMetaSession(monitor, db2source, "Read EXPLAIN tables");

        return new UITask<DB2PlanConfig>() {
            @Override
            protected DB2PlanConfig runTask() {
                // No valid explain tables found, propose to create them in current authId
                String msg = String.format(DB2Messages.dialog_explain_ask_to_create, object.getSessionUserSchema());
                if (!UIUtils.confirmAction(DB2Messages.dialog_explain_no_tables, msg)) {
                    return null;
                }
                // Ask the user in what tablespace to create the Explain tables and build a dialog with the list of usable tablespaces for the user to choose
                DB2TablespaceChooser tsChooserDialog = null;
                try {
                    final List<String> listTablespaces = DB2Utils.getListOfUsableTsForExplain(monitor, session);
                    if (listTablespaces.isEmpty()) {
                        DBWorkbench.getPlatformUI().showError(DB2Messages.dialog_explain_no_tablespace_found_title,
                                DB2Messages.dialog_explain_no_tablespace_found_title);
                        return null;
                    }
                    tsChooserDialog = new DB2TablespaceChooser(
                            UIUtils.getActiveWorkbenchShell(),
                            listTablespaces);
                } catch (SQLException e) {
                    log.error(e);
                }
                if (tsChooserDialog != null && tsChooserDialog.open() == IDialogConstants.OK_ID) {
                    object.setTablespace(tsChooserDialog.getSelectedTablespace());
                    return object;
                } else {
                    return null;
                }
            }
        }.execute();
    }
}
