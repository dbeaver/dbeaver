/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public void dispose()
    {
        sessionTable.dispose();
        UIUtils.dispose(boldFont);
    }

    public SessionManagerViewer(IWorkbenchPart part, Composite parent, final DBAServerSessionManager sessionManager) {

        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sash = UIUtils.createPartDivider(part, composite, SWT.VERTICAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        sessionTable = new SessionListControl(sash, sessionManager);
        sessionTable.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event)
            {
                onSessionSelect(getSelectedSession());
            }
        });

        sessionTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sessionTable.createProgressPanel(composite);


        sessionInfo = new Text(sash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        sessionInfo.setEditable(false);
        sessionInfo.setLayoutData(new GridData(GridData.FILL_BOTH));

        sash.setWeights(new int[]{70, 30});
    }

    protected void onSessionSelect(DBAServerSession session)
    {
        if (session == null) {
            sessionInfo.setText("");
        } else {
            final String activeQuery = session.getActiveQuery();
            sessionInfo.setText(CommonUtils.getString(activeQuery));
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
            super(sash, SWT.NONE, sessionManager);
            this.sessionManager = sessionManager;
        }

        @Override
        public Composite createProgressPanel(Composite container)
        {
            Composite infoGroup = super.createProgressPanel(container);

            ToolBarManager toolBar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
            contributeToToolbar(sessionManager, toolBar);
            toolBar.add(new Action("Refresh sessions", DBIcon.REFRESH.getImageDescriptor()) {
                @Override
                public void run()
                {
                    refreshSessions();
                }
            });

            toolBar.createControl(infoGroup);

            return infoGroup;
        }
    }
}