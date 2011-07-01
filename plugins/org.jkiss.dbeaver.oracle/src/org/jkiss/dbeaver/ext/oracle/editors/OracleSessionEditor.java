/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.utils.CommonUtils;
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
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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
 * OracleSessionEditor
 */
public class OracleSessionEditor extends SinglePageDatabaseEditor<IDatabaseNodeEditorInput>
{
    static final Log log = LogFactory.getLog(OracleSessionEditor.class);

    private static class SessionInfo {
        String sid;
        String user;
        String schema;
        String state;
        String sql;
        String event;
        String remoteHost;
        String remoteUser;
        String remoteProgram;

        private SessionInfo(String sid, String user, String schema, String state, String sql, String event, String remoteHost, String remoteUser, String remoteProgram)
        {
            this.sid = sid;
            this.user = user;
            this.schema = schema;
            this.state = state;
            this.sql = sql;
            this.event = event;
            this.remoteHost = remoteHost;
            this.remoteUser = remoteUser;
            this.remoteProgram = remoteProgram;
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

        sash.setWeights(new int[]{70, 30});

        pageControl = new PageControl(composite, SWT.NONE);
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
                    if (session.sql == null) {
                        sessionInfo.setText("");
                    } else {
                        sessionInfo.setText(session.sql);
                    }
                    pageControl.killSessionButton.setEnabled(false);
                    pageControl.killQueryButton.setEnabled(false);
                } else {
                    sessionInfo.setText("");
                    pageControl.killSessionButton.setEnabled(false);
                    pageControl.killQueryButton.setEnabled(false);
                }
            }
        });

        UIUtils.createTableColumn(table, SWT.LEFT, "SID");
        UIUtils.createTableColumn(table, SWT.LEFT, "User");
        UIUtils.createTableColumn(table, SWT.LEFT, "Schema");
        UIUtils.createTableColumn(table, SWT.LEFT, "State");
        UIUtils.createTableColumn(table, SWT.LEFT, "SQL");
        UIUtils.createTableColumn(table, SWT.LEFT, "Event");
        UIUtils.createTableColumn(table, SWT.LEFT, "Remote Host");
        UIUtils.createTableColumn(table, SWT.LEFT, "Remote User");
        UIUtils.createTableColumn(table, SWT.LEFT, "Remote Program");
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
                        JDBCPreparedStatement dbStat = context.prepareStatement(
                            "SELECT s.*,sq.SQL_TEXT FROM V$SESSION s\n" +
                            "LEFT OUTER JOIN V$SQL sq ON sq.SQL_ID=s.SQL_ID\n" +
                            "WHERE s.TYPE='USER'");
                        try {
                            JDBCResultSet dbResult = dbStat.executeQuery();
                            try {
                                List<SessionInfo> sessions = new ArrayList<SessionInfo>();
                                while (dbResult.next()) {
                                    SessionInfo session = new SessionInfo(
                                        JDBCUtils.safeGetString(dbResult, "SID"),
                                        JDBCUtils.safeGetString(dbResult, "USERNAME"),
                                        JDBCUtils.safeGetString(dbResult, "SCHEMANAME"),
                                        JDBCUtils.safeGetString(dbResult, "STATE"),
                                        JDBCUtils.safeGetString(dbResult, "SQL_TEXT"),
                                        JDBCUtils.safeGetString(dbResult, "EVENT"),
                                        JDBCUtils.safeGetString(dbResult, "MACHINE"),
                                        JDBCUtils.safeGetString(dbResult, "OSUSER"),
                                        JDBCUtils.safeGetString(dbResult, "PROGRAM")
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
            new DatabaseLoadService<SessionInfo>("Kill " + (killConnection ? ("session " + session.sid) : ("query " + session.event)), getDataSource()) {
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
                                "KILL CONNECTION " + session.sid :
                                "KILL QUERY " + session.sid);
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

    public OracleDataSource getDataSource()
    {
        DBPDataSource dataSource = super.getDataSource();
        if (dataSource == null) {
            return null;
        }
        if (dataSource instanceof OracleDataSource) {
            return (OracleDataSource)dataSource;
        }
        log.error("Bad datasource object: " + dataSource);
        return null;
    }

    public void refreshPart(Object source)
    {
        refreshSessions();
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
                case 0: return session.sid;
                case 1: return session.user;
                case 2: return session.schema;
                case 3: return session.state;
                case 4: return session.sql;
                case 5: return session.event;
                case 6: return session.remoteHost;
                case 7: return session.remoteUser;
                case 8: return session.remoteProgram;
                default: return null;
            }
        }

        public Font getFont(Object element, int columnIndex)
        {
            return null;
        }
    }

    private class PageControl extends ProgressPageControl {

        Button killSessionButton;
        Button killQueryButton;

        public PageControl(Composite parent, int style) {
            super(parent, style);
        }

        @Override
        protected Composite createProgressPanel(Composite container) {
            Composite panel = super.createProgressPanel(container);

            Composite buttonsPanel = UIUtils.createPlaceholder(panel, 3);
            killSessionButton = new Button(buttonsPanel, SWT.PUSH);
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

            killQueryButton = new Button(buttonsPanel, SWT.PUSH);
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

            Button refreshButton = new Button(buttonsPanel, SWT.PUSH);
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
                    setInfo("Done (" + sessionInfo.sid + ")");
                }
            }
        }
    }


}