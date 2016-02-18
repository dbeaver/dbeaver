/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreSessionEditor
 */
public class PostgreSessionEditor extends AbstractSessionEditor
{
    private KillSessionAction terminateQueryAction;

    @Override
    public void createPartControl(Composite parent) {
        terminateQueryAction = new KillSessionAction();
        super.createPartControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer(this, parent, new PostgreSessionManager((PostgreDataSource) executionContext.getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
            {
                toolBar.add(terminateQueryAction);
                toolBar.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
            }
        };
    }


    private class KillSessionAction extends Action {
        public KillSessionAction()
        {
            super(
                "Terminate",
                PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP));
        }

        @Override
        public void run()
        {
            final DBAServerSession session = getSessionsViewer().getSelectedSession();
            if (session != null && UIUtils.confirmAction(getSite().getShell(),
                this.getText(),
                NLS.bind("Teminate session?", getText(), session)))
            {
                getSessionsViewer().alterSession(
                    getSessionsViewer().getSelectedSession(),
                    null);
            }
        }
    }

}