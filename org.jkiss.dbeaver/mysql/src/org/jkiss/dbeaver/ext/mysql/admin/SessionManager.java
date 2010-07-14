/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jkiss.utils.CommonUtils;

/**
 * FolderEditor
 */
public class SessionManager extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    static final Log log = LogFactory.getLog(SessionManager.class);

    private PageControl pageControl;
    private TableViewer sessionsViewer;
    private Text sessionInfo;

    public void createPartControl(Composite parent) {

        pageControl = new PageControl(parent, SWT.NONE, getSite().getPart());

        SashForm sash = new SashForm(pageControl, SWT.VERTICAL | SWT.SMOOTH);
        sash.setSashWidth(10);

        GridData gd = new GridData(GridData.FILL_BOTH);
        sash.setLayoutData(gd);

        createSessionViewer(sash);

        sessionInfo = new Text(sash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        sessionInfo.setEditable(false);
        gd = new GridData(GridData.FILL_BOTH);
        sessionInfo.setLayoutData(gd);

        sash.setWeights(new int[] {70, 30});

        pageControl.createProgressPanel();

        refreshSessions();
    }

    private void createSessionViewer(SashForm sash) {
        GridData gd;
        sessionsViewer = new TableViewer(sash, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
        final Table table = sessionsViewer.getTable();
        table.setLinesVisible (true);
        table.setHeaderVisible(true);
        gd = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gd);
        sessionsViewer.setContentProvider(new IStructuredContentProvider()
        {
            public void dispose()
            {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
            {
            }
            public Object[] getElements(Object inputElement)
            {
                if (inputElement instanceof List) {
                    return ((List)inputElement).toArray();
                }
                return null;
            }
        });
        sessionsViewer.setLabelProvider(new SessionLabelProvider());
        sessionsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                if (sessionInfo == null || sessionInfo.isDisposed()) {
                    return;
                }
                SessionInfo session = getSelectedSession();
                if (session != null) {
                    if (session.info == null) {
                        sessionInfo.setText("");
                    } else {
                        sessionInfo.setText(session.info);
                    }
                    pageControl.killSessionButton.setEnabled(true);
                    pageControl.killQueryButton.setEnabled(!CommonUtils.isEmpty(session.info));
                } else {
                    sessionInfo.setText("");
                    pageControl.killSessionButton.setEnabled(false);
                    pageControl.killQueryButton.setEnabled(false);
                }
            }
        });

        UIUtils.createTableColumn(table, SWT.LEFT, "PID");
        UIUtils.createTableColumn(table, SWT.LEFT, "User");
        UIUtils.createTableColumn(table, SWT.LEFT, "Host");
        UIUtils.createTableColumn(table, SWT.LEFT, "DB");
        UIUtils.createTableColumn(table, SWT.LEFT, "Command");
        UIUtils.createTableColumn(table, SWT.LEFT, "Time");
        UIUtils.createTableColumn(table, SWT.LEFT, "State");
        UIUtils.packColumns(table);
    }

    SessionInfo getSelectedSession()
    {
        ISelection selection = sessionsViewer.getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            return (SessionInfo)((IStructuredSelection) selection).getFirstElement();
        } else {
            return null;
        }
    }

    private static class SessionInfo {
        String pid;
        String user;
        String host;
        String db;
        String command;
        String time;
        String state;
        String info;

        private SessionInfo(String pid, String user, String host, String db, String command, String time, String state, String info) {
            this.pid = pid;
            this.user = user;
            this.host = host;
            this.db = db;
            this.command = command;
            this.time = time;
            this.state = state;
            this.info = info;
        }
    }

    private void refreshSessions()
    {
        LoadingUtils.executeService(
            new AbstractLoadService<List<SessionInfo>>("Load active session list") {
                public List<SessionInfo> evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    if (getDataSource() == null) {
                        return null;
                    }
                    JDBCExecutionContext context = getDataSource().openContext(this.getProgressMonitor());
                    try {
                        JDBCPreparedStatement dbStat = context.prepareStatement("SHOW FULL PROCESSLIST");
                        JDBCResultSet dbResult = dbStat.executeQuery();
                        try {
                            List<SessionInfo> sessions = new ArrayList<SessionInfo>();
                            while (dbResult.next()) {
                                SessionInfo session = new SessionInfo(
                                    JDBCUtils.safeGetString(dbResult, "id"),
                                    JDBCUtils.safeGetString(dbResult, "user"),
                                    JDBCUtils.safeGetString(dbResult, "host"),
                                    JDBCUtils.safeGetString(dbResult, "db"),
                                    JDBCUtils.safeGetString(dbResult, "command"),
                                    JDBCUtils.safeGetString(dbResult, "time"),
                                    JDBCUtils.safeGetString(dbResult, "state"),
                                    JDBCUtils.safeGetString(dbResult, "info")
                                );
                                sessions.add(session);
                            }
                            return sessions;
                        } finally {
                            dbResult.close();
                        }
                    }
                    catch (SQLException e) {
                        throw new InvocationTargetException(e);
                    }
                    finally {
                        context.close();
                    }
                }
            },
            pageControl.createVisualizer());
    }

    private void killSession(final SessionInfo session, final boolean killConnection) {
        if (!UIUtils.confirmAction(
            getSite().getShell(),
            killConnection ? "Kill session" : "Terminate query",
            "Are you sure?"))
        {
            return;
        }

        LoadingUtils.executeService(
            new AbstractLoadService<SessionInfo>("Kill " + (killConnection ? ("session " + session.pid) : ("query " + session.info))) {
                public SessionInfo evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    if (getDataSource() == null) {
                        return null;
                    }
                    JDBCExecutionContext context = getDataSource().openContext(this.getProgressMonitor());
                    try {
                        JDBCPreparedStatement dbStat = context.prepareStatement(
                            killConnection ?
                                "KILL CONNECTION " + session.pid :
                                "KILL QUERY " + session.pid);
                        dbStat.execute();
                        return session;
                    }
                    catch (SQLException e) {
                        log.debug(e);
                        // just ignore it
                        return session;
                    }
                    finally {
                        context.close();
                    }
                }
            },
            pageControl.createKillVisualizer());
    }

    public MySQLDataSource getDataSource()
    {
        DBPDataSource dataSource = super.getDataSource();
        if (dataSource == null) {
            return null;
        }
        if (dataSource instanceof MySQLDataSource) {
            return (MySQLDataSource)dataSource;
        }
        log.error("Bad datasource object: " + dataSource);
        return null;
    }

    class SessionLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            SessionInfo session = (SessionInfo) element;
            switch (columnIndex) {
                case 0: return session.pid;
                case 1: return session.user;
                case 2: return session.host;
                case 3: return session.db;
                case 4: return session.command;
                case 5: return session.time;
                case 6: return session.state;
                case 7: return session.info;
                default: return null;
            }
        }

    }

    private class PageControl extends ProgressPageControl {

        Button killSessionButton;
        Button killQueryButton;

        public PageControl(Composite parent, int style, IWorkbenchPart workbenchPart) {
            super(parent, style, workbenchPart);
        }

        @Override
        protected int getProgressCellCount() {
            return 5;
        }

        @Override
        protected Composite createProgressPanel(Composite container) {
            Composite panel = super.createProgressPanel(container);

            killSessionButton = new Button(panel, SWT.PUSH);
            killSessionButton.setText("Kill session");
            killSessionButton.setEnabled(false);
            killSessionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SessionInfo session = getSelectedSession();
                    if (session != null) {
                        killSession(session, true);
                    }
                }
            });

            killQueryButton = new Button(panel, SWT.PUSH);
            killQueryButton.setText("Terminsate query");
            killQueryButton.setEnabled(false);
            killQueryButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SessionInfo session = getSelectedSession();
                    if (session != null) {
                        killSession(session, false);
                    }
                }
            });

            Button refreshButton = new Button(panel, SWT.PUSH);
            refreshButton.setText("Refresh sessions");
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    refreshSessions();
                }
            });

            return panel;
        }

        public ProgressVisualizer<List<SessionInfo>> createVisualizer() {
            return new SessionsVisualizer();
        }

        public ProgressVisualizer<SessionInfo> createKillVisualizer() {
            return new KillVisualizer() {};
        }

        private class SessionsVisualizer extends ProgressVisualizer<List<SessionInfo>> {
            @Override
            public void completeLoading(List<SessionInfo> sessionInfos) {
                super.completeLoading(sessionInfos);
                if (sessionInfos == null) {
                    return;
                }
                Table table = sessionsViewer.getTable();
                table.setRedraw(false);
                try {
                    sessionsViewer.setInput(sessionInfos);
                    UIUtils.packColumns(table);
                    //UIUtils.maxTableColumnsWidth(table);
                    setInfo(sessionInfos.size() + " sessions");
                } finally {
                    table.setRedraw(true);
                }
            }
        }

        private class KillVisualizer extends ProgressVisualizer<SessionInfo> {
            @Override
            public void completeLoading(SessionInfo sessionInfo) {
                super.completeLoading(sessionInfo);
                if (sessionInfo != null) {
                    setInfo("Done (" + sessionInfo.pid + ")");
                }
            }
        }
    }


}