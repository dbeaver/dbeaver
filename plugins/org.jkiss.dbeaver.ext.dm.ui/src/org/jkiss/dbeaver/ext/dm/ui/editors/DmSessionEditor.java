package org.jkiss.dbeaver.ext.dm.ui.editors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.session.DmServerSession;
import org.jkiss.dbeaver.ext.dm.model.session.DmServerSessionManager;
import org.jkiss.dbeaver.ext.dm.ui.internal.DmUIMessages;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

public class DmSessionEditor extends AbstractSessionEditor
{
    private DisconnectSessionAction killSessionAction;
    private DisconnectSessionAction disconnectSessionAction;

    public DmSessionEditor() {
    }

    @Override
    public void createEditorControl(Composite parent) {
        killSessionAction = new DisconnectSessionAction(true);
        disconnectSessionAction = new DisconnectSessionAction(false);
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer<DmServerSession>(this, parent, new DmServerSessionManager((DmDataSource) executionContext.getDataSource())) {
            private boolean showBackground;
            private boolean showInactive;

            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(killSessionAction);
                contributionManager.add(disconnectSessionAction);
                contributionManager.add(new Separator());

                contributionManager.add(ActionUtils.makeActionContribution(
                    new Action("Show background", Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                            setToolTipText("Show background tasks");
                            setChecked(showBackground);
                        }
                        @Override
                        public void run() {
                            showBackground = isChecked();
                            refreshPart(DmSessionEditor.this, true);
                        }
                    }, true));

                contributionManager.add(ActionUtils.makeActionContribution(
                    new Action("Show inactive", Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                            setToolTipText("Show inactive sessions");
                            setChecked(showInactive);
                        }
                        @Override
                        public void run() {
                            showInactive = isChecked();
                            refreshPart(DmSessionEditor.this, true);
                        }
                    }, true));
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
                disconnectSessionAction.setEnabled(session != null);
            }

            @Override
            protected void loadSettings(IDialogSettings settings) {
                showBackground = CommonUtils.toBoolean(settings.get("showBackground"));
                showInactive = CommonUtils.toBoolean(settings.get("showInactive"));
                super.loadSettings(settings);
            }

            @Override
            protected void saveSettings(IDialogSettings settings) {
                super.saveSettings(settings);
                settings.put("showBackground", showBackground);
                settings.put("showInactive", showInactive);
            }

            @Override
            public Map<String, Object> getSessionOptions() {
                Map<String, Object> options = new HashMap<>();
                if (showBackground) {
                    options.put(DmServerSessionManager.OPTION_SHOW_BACKGROUND, true);
                }
                if (showInactive) {
                    options.put(DmServerSessionManager.OPTION_SHOW_INACTIVE, true);
                }
                return options;
            }

        };
    }

    private class DisconnectSessionAction extends Action {
        private final boolean kill;
        DisconnectSessionAction(boolean kill)
        {
            super(
                kill ? DmUIMessages.editors_dm_session_editor_title_kill_session : "断开会话",
                DBeaverIcons.getImageDescriptor(kill ? UIIcon.REJECT : UIIcon.SQL_DISCONNECT));
            this.kill = kill;
        }

        @Override
        public void run()
        {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            final String action = (kill ? DmUIMessages.editors_dm_session_editor_action_kill : DmUIMessages.editors_dm_session_editor_action_disconnect) + DmUIMessages.editors_dm_session_editor_action__session;
            ConfirmationDialog dialog = new ConfirmationDialog(
                getSite().getShell(),
                action,
                null,
                NLS.bind(DmUIMessages.editors_dm_session_editor_confirm_action, action.toLowerCase(), sessions),
                MessageDialog.CONFIRM,
                new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                0,
                DmUIMessages.editors_dm_session_editor_confirm_title,
                false);
            if (dialog.open() == IDialogConstants.YES_ID) {
                Map<String, Object> options = new HashMap<>();
                if (kill) {
                    options.put(DmServerSessionManager.PROP_KILL_SESSION, kill);
                }
                if (dialog.getToggleState()) {
                    options.put(DmServerSessionManager.PROP_IMMEDIATE, true);
                }
                getSessionsViewer().alterSessions(sessions, options);
            }
        }
    }

}
