/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

/**
 * OracleSessionEditor
 */
public class OracleSessionEditor extends SinglePageDatabaseEditor<IDatabaseNodeEditorInput>
{
    private SessionManagerViewer sessionsViewer;

    @Override
    public void dispose()
    {
        sessionsViewer.dispose();
        super.dispose();
    }

    public void createPartControl(Composite parent) {
        sessionsViewer = new SessionManagerViewer(this, parent, new OracleServerSessionManager((OracleDataSource) getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
            {
                //toolBar.add(killSessionAction);
                //toolBar.add(terminateQueryAction);
                toolBar.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                //killSessionAction.setEnabled(session != null);
                //terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
            }
        };

        sessionsViewer.refreshSessions();
    }

    public void refreshPart(Object source)
    {
        sessionsViewer.refreshSessions();
    }
}