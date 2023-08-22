package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.editors.data.AbstractDataEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/5 11:56
 */
public class SchedulerJobLogEditor extends AbstractDataEditor<YashanDBSchedulerJob>
{
    private static final Log log = Log.getLog(SchedulerJobLogEditor.class);

    private static final String LOG_VIEW_NAME = "SCHEDULER_JOB_RUN_DETAILS";

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return getJobLogView();
    }

    @Override
    protected DBDDataFilter getEditorDataFilter() {
        YashanDBSchedulerJob job = getDatabaseObject();
        YashanDBTableBase logView = getJobLogView();
        if (logView == null) {
            return null;
        }
        List<DBDAttributeConstraint> constraints = new ArrayList<>();
        try {
            DBRProgressMonitor monitor = new VoidProgressMonitor();
            YashanDBTableColumn ownerAttr = logView.getAttribute(monitor, "OWNER");
            if (ownerAttr != null) {
                DBDAttributeConstraint ac = new DBDAttributeConstraint(ownerAttr, ownerAttr.getOrdinalPosition());
                ac.setVisible(false);
                ac.setOperator(DBCLogicalOperator.EQUALS);
                ac.setValue(job.getOwner());
                constraints.add(ac);
            }
            YashanDBTableColumn jobNameAttr = logView.getAttribute(monitor, "JOB_NAME");
            if (jobNameAttr != null) {
                DBDAttributeConstraint ac = new DBDAttributeConstraint(jobNameAttr, jobNameAttr.getOrdinalPosition());
                ac.setVisible(false);
                ac.setOperator(DBCLogicalOperator.EQUALS);
                ac.setValue(job.getName());
                constraints.add(ac);
            }
            YashanDBTableColumn logDateAttr = logView.getAttribute(monitor, "LOG_DATE");
            if (logDateAttr != null) {
                DBDAttributeConstraint ac = new DBDAttributeConstraint(logDateAttr, logDateAttr.getOrdinalPosition());
                ac.setOrderPosition(1);
                ac.setOrderDescending(true);
                ac.setVisible(true);
                constraints.add(ac);
            }
        } catch (DBException e) {
            log.error(e);
        }

        return new DBDDataFilter(constraints);
    }

    @Override
    protected boolean isSuspendDataQuery() {
        return false;
    }

    @Override
    protected String getDataQueryMessage() {
        return "Query job logs...";
    }

    @Override
    public boolean isReadyToRun() {
        return getJobLogView() != null;
    }

    private YashanDBTableBase getJobLogView() {
        DBRProgressMonitor monitor = new VoidProgressMonitor();

        try {
            YashanDBDataSource dataSource = getDatabaseObject().getDataSource();
            YashanDBSchema systemSchema = dataSource.getSchema(monitor, YashanDBConstants.SCHEMA_SYS);
            if (systemSchema != null) {
                return systemSchema.getView(monitor, YashanDBUtils.getSysUserViewName(monitor, dataSource, LOG_VIEW_NAME));
            }
            return null;
        } catch (DBException e) {
            log.error("Can't find log table", e);
            return null;
        }
    }
}

