/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUser>
{

    void loadGrants()
    {
        LoadingJob.createService(
            new DatabaseLoadService<List<MySQLGrant>>(MySQLUIMessages.editors_user_editor_abstract_load_grants, getDatabaseObject().getDataSource()) {
                @Override
                public java.util.List<MySQLGrant> evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        return getDatabaseObject().getGrants(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            getPageControl().createGrantsLoadVisualizer())
            .schedule();
    }

    @Override
    public void setFocus()
    {
        if (getPageControl() != null) {
            getPageControl().setFocus();
        }
    }

    protected abstract UserPageControl getPageControl();
    protected abstract void processGrants(List<MySQLGrant> grants);

    /**
     * Reads the user grants from the server (SHOW GRANTS) and shows them as an SQL script.
     */
    private void showGrantsScript() {
        final MySQLUser user = getDatabaseObject();
        final StringBuilder script = new StringBuilder();
        // Escape the account name (single quotes in user/host must be doubled)
        final String accountName = "'" + user.getUserName().replace("'", "''") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "'@'" + user.getHost().replace("'", "''") + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        try {
            UIUtils.runInProgressService(monitor -> {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, user, "Read user grants")) {
                    try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW GRANTS FOR " + accountName)) { //$NON-NLS-1$
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            while (dbResult.next()) {
                                String grant = JDBCUtils.safeGetString(dbResult, 1);
                                if (!CommonUtils.isEmpty(grant)) {
                                    script.append(grant).append(";\n"); //$NON-NLS-1$
                                }
                            }
                        }
                    }
                } catch (SQLException | DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                MySQLUIMessages.editors_user_editor_abstract_dialog_grants_script_title,
                NLS.bind(MySQLUIMessages.editors_user_editor_abstract_grants_script_error, user.getName()),
                e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        ViewSQLDialog dialog = new ViewSQLDialog(
            getSite(),
            () -> DBUtils.getDefaultContext(user, true),
            MySQLUIMessages.editors_user_editor_abstract_dialog_grants_script_title + " - " + user.getName(), //$NON-NLS-1$
            UIIcon.SQL_SCRIPT,
            script.toString());
        dialog.open();
    }

    protected class UserPageControl extends ObjectEditorPageControl {
        public UserPageControl(Composite parent) {
            super(parent, SWT.NONE, MySQLUserEditorAbstract.this);
        }

        public ProgressVisualizer<List<MySQLGrant>> createGrantsLoadVisualizer() {
            return new ProgressVisualizer<List<MySQLGrant>>() {
                @Override
                public void completeLoading(@Nullable List<MySQLGrant> grants) {
                    super.completeLoading(grants);
                    processGrants(grants);
                }
            };
        }

        @Override
        public void fillCustomActions(@NotNull IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            Action scriptAction = new Action(
                MySQLUIMessages.editors_user_editor_abstract_action_grants_script,
                DBeaverIcons.getImageDescriptor(UIIcon.SQL_SCRIPT)
            ) {
                @Override
                public void run() {
                    showGrantsScript();
                }
            };
            scriptAction.setToolTipText(MySQLUIMessages.editors_user_editor_abstract_dialog_grants_script_title);
            ActionContributionItem scriptItem = new ActionContributionItem(scriptAction);
            scriptItem.setMode(ActionContributionItem.MODE_FORCE_TEXT);
            contributionManager.add(scriptItem);
            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
        }
    }

}