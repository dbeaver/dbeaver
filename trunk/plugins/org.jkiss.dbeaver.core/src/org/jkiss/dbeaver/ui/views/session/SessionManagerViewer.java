/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * SessionManagerViewer
 */
public class SessionManagerViewer
{
    private SessionListControl sessionTable;
    private Text sessionInfo;
    private Font boldFont;
    private PropertyTreeViewer sessionProps;

    public void dispose()
    {
        sessionTable.disposeControl();
        UIUtils.dispose(boldFont);
    }

    public SessionManagerViewer(IWorkbenchPart part, Composite parent, final DBAServerSessionManager sessionManager) {

        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sash = UIUtils.createPartDivider(part, composite, SWT.VERTICAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        sessionTable = new SessionListControl(sash, sessionManager);
        sessionTable.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                onSessionSelect(getSelectedSession());
            }
        });

        sessionTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sessionTable.createProgressPanel(composite);

        {
            SashForm infoSash = UIUtils.createPartDivider(part, sash, SWT.HORIZONTAL | SWT.SMOOTH);
            infoSash.setLayoutData(new GridData(GridData.FILL_BOTH));

            sessionInfo = new Text(infoSash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
            sessionInfo.setEditable(false);
            sessionInfo.setLayoutData(new GridData(GridData.FILL_BOTH));

            sessionProps = new PropertyTreeViewer(infoSash, SWT.BORDER);

            sash.setWeights(new int[]{50, 50});
        }

        sash.setWeights(new int[]{70, 30});
    }

    protected void onSessionSelect(DBAServerSession session)
    {
        if (session == null) {
            sessionInfo.setText("");

            sessionProps.clearProperties();
        } else {
            final String activeQuery = session.getActiveQuery();
            sessionInfo.setText(CommonUtils.notEmpty(activeQuery));

            PropertyCollector propCollector = new PropertyCollector(session, true);
            propCollector.collectProperties();
            sessionProps.loadProperties(propCollector);
        }
    }

    protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar)
    {

    }

    public DBAServerSession getSelectedSession()
    {
        ISelection selection = sessionTable.getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            return (DBAServerSession)((IStructuredSelection) selection).getFirstElement();
        } else {
            return null;
        }
    }

    public void refreshSessions()
    {
        sessionTable.loadData();
        onSessionSelect(null);
    }

    public void alterSession(final DBAServerSession session, Map<String, Object> options) {
        sessionTable.createAlterService(session, options).schedule();
    }

    private class SessionListControl extends SessionTable {
        private final DBAServerSessionManager sessionManager;

        public SessionListControl(SashForm sash, DBAServerSessionManager sessionManager)
        {
            super(sash, SWT.SHEET, sessionManager);
            this.sessionManager = sessionManager;
        }

        @Override
        protected void fillCustomToolbar(ToolBarManager toolbarManager) {
            contributeToToolbar(sessionManager, toolbarManager);
            toolbarManager.add(new Action("Refresh sessions", DBIcon.REFRESH.getImageDescriptor()) {
                @Override
                public void run()
                {
                    refreshSessions();
                }
            });
        }
    }
}