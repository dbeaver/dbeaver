/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQLSessionEditor
 */
public class MySQLSessionEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    static final Log log = LogFactory.getLog(MySQLSessionEditor.class);

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

    private PageControl pageControl;
    private TableViewer sessionsViewer;
    private Text sessionInfo;
    private Font boldFont;

    @Override
    public void dispose()
    {
        UIUtils.dispose(boldFont);
        super.dispose();
    }

    public void createPartControl(Composite parent) {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sash = UIUtils.createPartDivider(this, composite, SWT.VERTICAL | SWT.SMOOTH);

        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        createSessionViewer(sash);

        sessionInfo = new Text(sash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        sessionInfo.setEditable(false);
        sessionInfo.setLayoutData(new GridData(GridData.FILL_BOTH));

        sash.setWeights(new int[] {70, 30});

        pageControl = new PageControl(composite, SWT.NONE, this);
        pageControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
        sessionsViewer.setContentProvider(new ListContentProvider());
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

    private void refreshSessions()
    {
        LoadingUtils.createService(
            new DatabaseLoadService<List<SessionInfo>>("Load active session list", getDataSource()) {
                public List<SessionInfo> evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    if (getDataSource() == null) {
                        return null;
                    }
                    JDBCExecutionContext context = getDataSource().openContext(this.getProgressMonitor(), DBCExecutionPurpose.UTIL, "Retrieve process list");
                    try {
                        JDBCPreparedStatement dbStat = context.prepareStatement("SHOW FULL PROCESSLIST");
                        try {
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
                        } finally {
                            dbStat.close();
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
            pageControl.createRefreshVisualizer())
            .schedule();
    }

    private void killSession(final SessionInfo session, final boolean killConnection) {
        if (!UIUtils.confirmAction(
            getSite().getShell(),
            killConnection ? "Kill session" : "Terminate query",
            "Are you sure?"))
        {
            return;
        }

        LoadingUtils.createService(
            new DatabaseLoadService<SessionInfo>("Kill " + (killConnection ? ("session " + session.pid) : ("query " + session.info)), getDataSource()) {
                public SessionInfo evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    if (getDataSource() == null) {
                        return null;
                    }
                    JDBCExecutionContext context = getDataSource().openContext(this.getProgressMonitor(), DBCExecutionPurpose.UTIL, "Cancel active process");
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
            pageControl.createKillVisualizer())
            .schedule();
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

    public void refreshDatabaseContent(DBNEvent event) {

    }

    class SessionLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider
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

        public Font getFont(Object element, int columnIndex)
        {
            SessionInfo session = (SessionInfo) element;
            if (!CommonUtils.isEmpty(session.info)) {
                return boldFont;
            }
            return null;
        }
    }

    private class PageControl extends ProgressPageControl {

        Button killSessionButton;
        Button killQueryButton;

        public PageControl(Composite parent, int style, IWorkbenchPart workbenchPart) {
            super(parent, style, workbenchPart);
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
            killQueryButton.setText("Terminate query");
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

        public ProgressVisualizer<List<SessionInfo>> createRefreshVisualizer() {
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