/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * MySQLSessionEditor
 */
public class MySQLSessionEditor extends SinglePageDatabaseEditor<IDatabaseNodeEditorInput>
{
    static final Log log = LogFactory.getLog(MySQLSessionEditor.class);

    private SessionManagerViewer sessionsViewer;
    private KillSessionAction killSessionAction;
    private TerminateQueryAction terminateQueryAction;

    @Override
    public void dispose()
    {
        sessionsViewer.dispose();
        super.dispose();
    }

    public void createPartControl(Composite parent) {
        killSessionAction = new KillSessionAction();
        terminateQueryAction = new TerminateQueryAction();
        sessionsViewer = new SessionManagerViewer(this, parent, new MySQLSessionManager(getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
            {
                toolBar.add(killSessionAction);
                toolBar.add(terminateQueryAction);
                toolBar.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
                terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
            }
        };

        sessionsViewer.refreshSessions();
    }

    public MySQLDataSource getDataSource()
    {
        DBPDataSource dataSource = super.getDataSource();
        if (dataSource instanceof MySQLDataSource) {
            return (MySQLDataSource)dataSource;
        }
        log.error("Bad datasource object: " + dataSource);
        return null;
    }

    public void refreshPart(Object source)
    {
        sessionsViewer.refreshSessions();
    }

    private class KillSessionAction extends Action {
        public KillSessionAction()
        {
            super("Kill Session", DBIcon.SQL_DISCONNECT.getImageDescriptor());
        }

        @Override
        public void run()
        {
            sessionsViewer.alterSession(
                sessionsViewer.getSelectedSession(),
                Collections.singletonMap(MySQLSessionManager.PROP_KILL_QUERY, (Object)true));
        }
    }

    private class TerminateQueryAction extends Action {
        public TerminateQueryAction()
        {
            super("Terminate Query",
                PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP));
        }

        @Override
        public void run()
        {
            sessionsViewer.alterSession(
                sessionsViewer.getSelectedSession(),
                Collections.singletonMap(MySQLSessionManager.PROP_KILL_QUERY, (Object)false));
        }
    }
}