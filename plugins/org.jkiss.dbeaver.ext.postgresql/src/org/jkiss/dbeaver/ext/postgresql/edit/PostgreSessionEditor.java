/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSession;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * PostgreSessionEditor
 */
public class PostgreSessionEditor extends AbstractSessionEditor
{
    private KillSessionAction terminateQueryAction;

    @Override
    public void createEditorControl(Composite parent) {
        terminateQueryAction = new KillSessionAction();
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer<PostgreSession>(this, parent, new PostgreSessionManager((PostgreDataSource) executionContext.getDataSource())) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(terminateQueryAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                terminateQueryAction.setEnabled(session != null);
            }
        };
    }


    private class KillSessionAction extends Action {
        KillSessionAction()
        {
            super(
                "Terminate",
                UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP));
        }

        @Override
        public void run()
        {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            if (sessions != null && UIUtils.confirmAction(
                getSite().getShell(),
                this.getText(),
                NLS.bind("Terminate session {0}?", sessions)))
            {
                getSessionsViewer().alterSessions(
                    sessions,
                    null);
            }
        }
    }

}