/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSessionManager;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * PostgreSessionEditor
 */
public class PostgreSessionEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    static final Log log = Log.getLog(PostgreSessionEditor.class);

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
        sessionsViewer = new SessionManagerViewer(this, parent, new PostgreSessionManager((PostgreDataSource) getExecutionContext().getDataSource())) {
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
    public void refreshPart(Object source, boolean force)
    {
        sessionsViewer.refreshSessions();
    }

    private class KillSessionAction extends Action {
        private boolean killQuery;
        public KillSessionAction(boolean killQuery)
        {
            super(
                killQuery ? "Terminate" : "Kill",
                killQuery ?
                    PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP) :
                    DBeaverIcons.getImageDescriptor(UIIcon.SQL_DISCONNECT));
            this.killQuery = killQuery;
        }

        @Override
        public void run()
        {
            final DBAServerSession session = sessionsViewer.getSelectedSession();
            if (session != null && UIUtils.confirmAction(getSite().getShell(),
                this.getText(),
                NLS.bind("Teminate session?", getText(), session)))
            {
                sessionsViewer.alterSession(
                    sessionsViewer.getSelectedSession(),
                    Collections.singletonMap(PostgreSessionManager.PROP_KILL_QUERY, (Object)killQuery));
            }
        }
    }

}