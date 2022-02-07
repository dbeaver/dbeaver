/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ext.mssql.model.session.SQLServerSessionManager;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerUIMessages;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLServerSessionEditor
 */
public class SQLServerSessionEditor extends AbstractSessionEditor
{
    private KillSessionAction terminateQueryAction;
    private boolean showOnlyConnections = true;
    private Action onlyConnectionsAction;

    @Override
    public void createEditorControl(Composite parent) {
        terminateQueryAction = new KillSessionAction();
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        final SQLServerDataSource dataSource = (SQLServerDataSource) executionContext.getDataSource();
        return new SessionManagerViewer<>(this, parent, new SQLServerSessionManager(dataSource)) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager) {
                if (!dataSource.isBabelfish()) {
                    onlyConnectionsAction = new Action(SQLServerUIMessages.session_editor_action_show_connections_text, Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                            setToolTipText(SQLServerUIMessages.session_editor_action_show_connections_tip);
                            setChecked(showOnlyConnections);
                        }

                        @Override
                        public void run() {
                            showOnlyConnections = isChecked();
                            refreshPart(SQLServerSessionEditor.this, true);
                        }
                    };
                    contributionManager.add(ActionUtils.makeActionContribution(onlyConnectionsAction, true));
                    contributionManager.add(new Separator());
                }
                contributionManager.add(terminateQueryAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                terminateQueryAction.setEnabled(session != null);
            }

            @Override
            protected void loadSettings(IDialogSettings settings) {
                if (onlyConnectionsAction!= null) {
                    showOnlyConnections = CommonUtils.getBoolean(settings.get("showOnlyConnections"), true);
                }
                super.loadSettings(settings);
            }

            @Override
            protected void saveSettings(IDialogSettings settings) {
                super.saveSettings(settings);
                if (onlyConnectionsAction != null) {
                    settings.put("showOnlyConnections", showOnlyConnections);
                }
            }

            @Override
            public Map<String, Object> getSessionOptions() {
                Map<String, Object> options = new HashMap<>();
                if (showOnlyConnections) {
                    options.put(SQLServerSessionManager.OPTION_SHOW_ONLY_CONNECTIONS, true);
                }
                return options;
            }

        };
    }


    private class KillSessionAction extends Action {
        KillSessionAction()
        {
            super(
                SQLServerUIMessages.session_editor_action_kill_session_text,
                UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP));
        }

        @Override
        public void run()
        {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            if (sessions != null && UIUtils.confirmAction(
                getSite().getShell(),
                this.getText(),
                NLS.bind(SQLServerUIMessages.session_editor_action_kill_session_message, sessions)))
            {
                getSessionsViewer().alterSessions(
                    sessions,
                    null);
            }
        }
    }

}