package org.jkiss.dbeaver.ext.cubrid.ui.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.ui.internal.CubridMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class CubridInfoPanel implements IResultSetPanel
{

    private static final Log log = Log.getLog(CubridInfoPanel.class);
    private Table table;
    private Composite control;
    private Text plainText;
    private Text statisticInfo;
    private IResultSetPresentation presentation;

    @Override
    public void contributeActions(IContributionManager manager) {
        // not implemented
    }

    @Override
    public Control createContents(IResultSetPresentation presentation, Composite parent) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        this.presentation = presentation;
        control = UIUtils.createPlaceholder(parent, 1);
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        SashForm planPanel = new CustomSashForm(control, SWT.VERTICAL);
        planPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        final GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        planPanel.setLayout(gl);
        if (!CommonUtils.isEmpty(store.getString(CubridConstants.STATISTIC))) {
            table = new Table(planPanel, SWT.MULTI | SWT.FULL_SELECTION);
            table.setLinesVisible(!UIStyles.isDarkTheme());
            table.setHeaderVisible(true);
            table.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createTableColumn(table, SWT.LEFT, "Name");
            UIUtils.createTableColumn(table, SWT.LEFT, "Value");
        } else {
            statisticInfo = new Text(planPanel, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
            statisticInfo.setText(String.format(CubridMessages.statistic_instruction_message, CubridMessages.statistic_info + "|" + CubridMessages.statistic_all_info));
        }
        plainText = new Text(planPanel, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        plainText.setText(String.format(CubridMessages.statistic_instruction_message, CubridMessages.statistic_trace_info));
        return control;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void activatePanel() {
        refreshResult();

    }

    @Override
    public void deactivatePanel() {
        if (this.control != null && !this.control.isDisposed()) {
            this.control.dispose();
        }
    }

    @Override
    public void setFocus() {
        this.control.setFocus();
    }

    @Override
    public void refresh(boolean force) {
        refreshResult();

    }

    @Nullable
    private String getStatisticQuery() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (store.getString(CubridConstants.STATISTIC).equals(CubridConstants.STATISTIC_INFO)) {
            return "show exec statistics;";
        } else if (store.getString(CubridConstants.STATISTIC).equals(CubridConstants.STATISTIC_ALL_INFO)) {
            return "show exec statistics all;";
        }
        return null;
    }

    private void showStatistic(JDBCStatement stm, String queryInfo) throws SQLException {
        table.removeAll();
        stm.execute(this.presentation.getController().getContainer().toString());
        try (JDBCResultSet resultSet = stm.executeQuery(queryInfo)) {
            while (resultSet.next()) {
                TableItem item = new TableItem(table, SWT.LEFT);
                item.setText(0, resultSet.getString("variable"));
                item.setText(1, resultSet.getString("value"));
            }
        }
        UIUtils.packColumns(table);
    }

    private void refreshResult() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        new AbstractJob("Read Statistic")
        {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {

                UIUtils.syncExec(
                        () -> {
                            try (JDBCSession session = DBUtils.openMetaSession(monitor, presentation.getController().getDataContainer().getDataSource(), "Read Statistic")) {
                                try (JDBCStatement stm = session.createStatement()) {
                                    String statisticQuery = getStatisticQuery();
                                    if (CommonUtils.isNotEmpty(statisticQuery)) {
                                        showStatistic(stm, statisticQuery);
                                    }
                                    if (store.getBoolean(CubridConstants.STATISTIC_TRACE)) {
                                        stm.execute(presentation.getController().getContainer().toString());
                                        try (JDBCResultSet resultSet = stm.executeQuery("show trace;")) {
                                            if (resultSet.next()) {
                                                String st = resultSet.getString("trace");
                                                plainText.setText(st);
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException | DBCException e) {
                                log.error("could not read statistic", e);
                            }
                        });
                return Status.OK_STATUS;
            }
        }.schedule();
    }

}
