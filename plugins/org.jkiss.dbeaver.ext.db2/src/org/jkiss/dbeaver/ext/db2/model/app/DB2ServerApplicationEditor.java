/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.app;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB2 Application Editor
 * 
 * @author Denis Forveille
 */
public class DB2ServerApplicationEditor extends AbstractSessionEditor {
    private ForceApplicationAction forceApplicationAction;

    @Override
    public void createEditorControl(Composite parent)
    {
        forceApplicationAction = new ForceApplicationAction();
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer<DB2ServerApplication>(this, parent, new DB2ServerApplicationManager((DB2DataSource) executionContext.getDataSource())) {

            @Override
            @SuppressWarnings("rawtypes")
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(forceApplicationAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                forceApplicationAction.setEnabled(session != null);
            }
        };
    }

    private class ForceApplicationAction extends Action {

        public ForceApplicationAction()
        {
            super(DB2Messages.editors_db2_application_editor_title_force_application, DBeaverIcons.getImageDescriptor(UIIcon.REJECT));
        }

        @Override
        public void run()
        {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            final String action = DB2Messages.editors_db2_application_editor_action_force;
            if (UIUtils.confirmAction(getSite().getShell(), "Confirm force application",
                NLS.bind(DB2Messages.editors_db2_application_editor_confirm_action, action.toLowerCase(), sessions))) {
                Map<String, Object> options = new HashMap<>();
                getSessionsViewer().alterSessions(sessions, options);
            }
        }
    }
}