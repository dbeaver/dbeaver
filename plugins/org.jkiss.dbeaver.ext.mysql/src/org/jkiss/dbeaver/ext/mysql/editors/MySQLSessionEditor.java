/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * MySQLSessionEditor
 */
public class MySQLSessionEditor extends AbstractSessionEditor
{
    private static final Log log = Log.getLog(MySQLSessionEditor.class);

    private KillSessionAction killSessionAction;
    private KillSessionAction terminateQueryAction;

    @Override
    public void createPartControl(Composite parent) {
        killSessionAction = new KillSessionAction(false);
        terminateQueryAction = new KillSessionAction(true);
        super.createPartControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer(this, parent, new MySQLSessionManager((MySQLDataSource) executionContext.getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(killSessionAction);
                contributionManager.add(terminateQueryAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
                terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
            }
        };
    }

    private class KillSessionAction extends Action {
        private boolean killQuery;
        public KillSessionAction(boolean killQuery)
        {
            super(
                killQuery ? MySQLMessages.editors_session_editor_action_terminate_Query : MySQLMessages.editors_session_editor_action_kill_Session,
                killQuery ?
                    UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP) :
                    DBeaverIcons.getImageDescriptor(UIIcon.SQL_DISCONNECT));
            this.killQuery = killQuery;
        }

        @Override
        public void run()
        {
            final DBAServerSession session = getSessionsViewer().getSelectedSession();
            if (session != null && UIUtils.confirmAction(getSite().getShell(),
                this.getText(),
                NLS.bind(MySQLMessages.editors_session_editor_confirm, getText(), session)))
            {
                getSessionsViewer().alterSession(
                    getSessionsViewer().getSelectedSession(),
                    Collections.singletonMap(MySQLSessionManager.PROP_KILL_QUERY, (Object) killQuery));
            }
        }
    }

}