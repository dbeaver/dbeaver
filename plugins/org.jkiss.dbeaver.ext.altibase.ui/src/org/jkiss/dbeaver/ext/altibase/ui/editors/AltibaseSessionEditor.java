/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.ext.altibase.model.session.AltibaseServerSession;
import org.jkiss.dbeaver.ext.altibase.model.session.AltibaseServerSessionManager;
import org.jkiss.dbeaver.ext.altibase.ui.internal.AltibaseUIMessages;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AltibaseSessionEditor
 */
public class AltibaseSessionEditor extends AbstractSessionEditor {
    private DisconnectSessionAction disconnectSessionAction;

    public AltibaseSessionEditor() {
    }

    @Override
    public void createEditorControl(Composite parent) {
        disconnectSessionAction = new DisconnectSessionAction();
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer<AltibaseServerSession>(this, parent, 
                new AltibaseServerSessionManager((AltibaseDataSource) executionContext.getDataSource())) {

            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager) {
                contributionManager.add(disconnectSessionAction);
                contributionManager.add(new Separator());
            }
            
            @Override
            protected void onSessionSelect(DBAServerSession session) {
                super.onSessionSelect(session);
                disconnectSessionAction.setEnabled(session != null);
            }
        };
    }

    private class DisconnectSessionAction extends Action {
        DisconnectSessionAction() {
            super(AltibaseUIMessages.editors_altibase_session_editor_action_disconnect_session, 
                 DBeaverIcons.getImageDescriptor(UIIcon.SQL_DISCONNECT));
        }

        @Override
        public void run() {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            final String action = AltibaseUIMessages.editors_altibase_session_editor_action_disconnect_session;
            ConfirmationDialog dialog = new ConfirmationDialog(
                    getSite().getShell(),
                    action,
                    null,
                    NLS.bind(AltibaseUIMessages.editors_altibase_session_editor_confirm_action, action.toLowerCase(), sessions),
                    MessageDialog.CONFIRM,
                    new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                    0,
                    null,
                    false);

            if (dialog.open() == IDialogConstants.YES_ID) {
                Map<String, Object> options = new HashMap<>();
                getSessionsViewer().alterSessions(sessions, options);
            }
            
            refreshPart(AltibaseSessionEditor.this, true);

        }
    }

}