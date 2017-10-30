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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * SessionManagerViewer
 */
public class SessionManagerViewer
{
    private SessionListControl sessionTable;
    //private Text sessionInfo;
    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;

    private Font boldFont;
    private PropertyTreeViewer sessionProps;
    private DBAServerSession curSession;

    public void dispose()
    {
        sessionTable.disposeControl();
        UIUtils.dispose(boldFont);
    }

    public SessionManagerViewer(IWorkbenchPart part, Composite parent, final DBAServerSessionManager sessionManager) {
        this.subSite = new SubEditorSite(part.getSite());
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sash = UIUtils.createPartDivider(part, composite, SWT.VERTICAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        sessionTable = new SessionListControl(sash, part.getSite(), sessionManager);
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


//            sessionInfo = new Text(infoSash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
//            sessionInfo.setEditable(false);
//            sessionInfo.setLayoutData(new GridData(GridData.FILL_BOTH));
            sqlViewer = new SQLEditorBase() {
                @Override
                public DBCExecutionContext getExecutionContext() {
                    return sessionManager.getDataSource().getDefaultContext(false);
                }

                @Override
                public boolean isFoldingEnabled() {
                    return false;
                }
            };
            updateSQL();
            sqlViewer.createPartControl(infoSash);
            Object text = sqlViewer.getAdapter(Control.class);
            if (text instanceof StyledText) {
                ((StyledText) text).setWordWrap(true);
            }

            sqlViewer.reloadSyntaxRules();

            parent.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    sqlViewer.dispose();
                }
            });


            sessionProps = new PropertyTreeViewer(infoSash, SWT.BORDER);

            sash.setWeights(new int[]{50, 50});
        }

        sash.setWeights(new int[]{70, 30});
    }

    protected void onSessionSelect(DBAServerSession session)
    {
        curSession = session;
        updateSQL();
        if (session == null) {
            sessionProps.clearProperties();
        } else {
            PropertyCollector propCollector = new PropertyCollector(session, true);
            propCollector.collectProperties();
            sessionProps.loadProperties(propCollector);
        }
    }

    protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
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

    protected void updateSQL() {
        try {
            String text = curSession == null ? "" : CommonUtils.notEmpty(curSession.getActiveQuery());
            StringEditorInput sqlInput = new StringEditorInput(sessionTable.getShell().getText(), text, true, GeneralUtils.getDefaultFileEncoding());
            sqlViewer.init(subSite, sqlInput);
            if (sqlViewer.getTextViewer() != null) {
                sqlViewer.reloadSyntaxRules();
            }
        } catch (PartInitException e) {
            DBUserInterface.getInstance().showError(sessionTable.getShell().getText(), null, e);
        }
    }

    public Composite getControl() {
        return sessionTable.getControl();
    }

    private class SessionListControl extends SessionTable {

        public SessionListControl(SashForm sash, IWorkbenchSite site, DBAServerSessionManager<DBAServerSession> sessionManager)
        {
            super(sash, SWT.SHEET, site, sessionManager);
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            contributeToToolbar(getSessionManager(), contributionManager);
            contributionManager.add(new Action("Refresh sessions", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH)) {
                @Override
                public void run()
                {
                    refreshSessions();
                }
            });
        }
    }

}