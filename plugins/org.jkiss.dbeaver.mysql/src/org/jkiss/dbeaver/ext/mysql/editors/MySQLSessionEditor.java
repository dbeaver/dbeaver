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
package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * MySQLSessionEditor
 */
public class MySQLSessionEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    static final Log log = Log.getLog(MySQLSessionEditor.class);

    private SessionManagerViewer sessionsViewer;
    private KillSessionAction killSessionAction;
    private KillSessionAction terminateQueryAction;

    @Override
    public void dispose()
    {
        sessionsViewer.dispose();
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        killSessionAction = new KillSessionAction(false);
        terminateQueryAction = new KillSessionAction(true);
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

    @Override
    public MySQLDataSource getDataSource()
    {
        DBPDataSource dataSource = super.getDataSource();
        if (dataSource instanceof MySQLDataSource) {
            return (MySQLDataSource)dataSource;
        }
        log.error("Bad datasource object: " + dataSource); //$NON-NLS-1$
        return null;
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        sessionsViewer.refreshSessions();
    }

    private class KillSessionAction extends Action {
        private boolean killQuery;
        public KillSessionAction(boolean killQuery)
        {
            super(
                killQuery ? MySQLMessages.editors_session_editor_action_terminate_Query : MySQLMessages.editors_session_editor_action_kill_Session,
                killQuery ?
                    PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP) :
                    DBIcon.SQL_DISCONNECT.getImageDescriptor());
            this.killQuery = killQuery;
        }

        @Override
        public void run()
        {
            final DBAServerSession session = sessionsViewer.getSelectedSession();
            if (session != null && UIUtils.confirmAction(getSite().getShell(),
                this.getText(),
                NLS.bind(MySQLMessages.editors_session_editor_confirm, getText(), session)))
            {
                sessionsViewer.alterSession(
                    sessionsViewer.getSelectedSession(),
                    Collections.singletonMap(MySQLSessionManager.PROP_KILL_QUERY, (Object)killQuery));
            }
        }
    }

}