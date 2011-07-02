/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

import java.util.HashMap;
import java.util.Map;

/**
 * OracleSessionEditor
 */
public class OracleSessionEditor extends SinglePageDatabaseEditor<IDatabaseNodeEditorInput>
{
    private SessionManagerViewer sessionsViewer;
    private DisconnectSessionAction killSessionAction;
    private DisconnectSessionAction disconnectSessionAction;

    @Override
    public void dispose()
    {
        sessionsViewer.dispose();
        super.dispose();
    }

    public void createPartControl(Composite parent) {
        killSessionAction = new DisconnectSessionAction(true);
        disconnectSessionAction = new DisconnectSessionAction(false);
        sessionsViewer = new SessionManagerViewer(this, parent, new OracleServerSessionManager((OracleDataSource) getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
            {
                toolBar.add(killSessionAction);
                toolBar.add(disconnectSessionAction);
                toolBar.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
                disconnectSessionAction.setEnabled(session != null);
            }
        };

        sessionsViewer.refreshSessions();
    }

    public void refreshPart(Object source)
    {
        sessionsViewer.refreshSessions();
    }

    private class DisconnectSessionAction extends Action {
        private final boolean kill;
        public DisconnectSessionAction(boolean kill)
        {
            super(
                kill ? "Kill Session" : "Disconnect session",
                kill ? DBIcon.REJECT.getImageDescriptor() : DBIcon.SQL_DISCONNECT.getImageDescriptor());
            this.kill = kill;
        }

        @Override
        public void run()
        {
            final DBAServerSession session = sessionsViewer.getSelectedSession();
            final String action = (kill ? "Kill" : "Disconnect") + " session";
            ConfirmationDialog dialog = new ConfirmationDialog(
                getSite().getShell(),
                action,
                null,
                "Are you sure you want to " + action.toLowerCase() + " '" + session + "'?",
                MessageDialog.CONFIRM,
                new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                0,
                "Immediate",
                false);
            if (dialog.open() == IDialogConstants.YES_ID) {
                Map<String, Object> options = new HashMap<String, Object>();
                if (kill) {
                    options.put(OracleServerSessionManager.PROP_KILL_SESSION, kill);
                }
                if (dialog.getToggleState()) {
                    options.put(OracleServerSessionManager.PROP_IMMEDIATE, true);
                }
                sessionsViewer.alterSession(session, options);
            }
        }
    }

}