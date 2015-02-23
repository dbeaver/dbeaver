/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.app;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2 Application Editor
 * 
 * @author Denis Forveille
 */
public class DB2ServerApplicationEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput> {
    private SessionManagerViewer applicationViewer;
    private ForceApplicationAction forceApplicationAction;

    @Override
    public void dispose()
    {
        applicationViewer.dispose();
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        forceApplicationAction = new ForceApplicationAction();
        applicationViewer = new SessionManagerViewer(this, parent, new DB2ServerApplicationManager((DB2DataSource) getDataSource())) {

            @Override
            @SuppressWarnings("rawtypes")
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
            {
                toolBar.add(forceApplicationAction);
                toolBar.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                forceApplicationAction.setEnabled(session != null);
            }
        };

        applicationViewer.refreshSessions();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        applicationViewer.refreshSessions();
    }

    private class ForceApplicationAction extends Action {

        public ForceApplicationAction()
        {
            super(DB2Messages.editors_db2_application_editor_title_force_application, DBIcon.REJECT.getImageDescriptor());
        }

        @Override
        public void run()
        {
            final DBAServerSession session = applicationViewer.getSelectedSession();
            final String action = DB2Messages.editors_db2_application_editor_action_force;
            if (UIUtils.confirmAction(getSite().getShell(), "Confirm force application",
                NLS.bind(DB2Messages.editors_db2_application_editor_confirm_action, action.toLowerCase(), session))) {
                Map<String, Object> options = new HashMap<String, Object>();
                applicationViewer.alterSession(session, options);
            }
        }
    }
}