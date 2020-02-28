/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.editors;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.*;
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
 * SchedulerJobLogEditor
 */
public class SchedulerJobLogEditor extends AbstractDataEditor<OracleSchedulerJob>
{
    private static final Log log = Log.getLog(SchedulerJobLogEditor.class);

    private static final String LOG_VIEW_NAME = "ALL_SCHEDULER_JOB_RUN_DETAILS";

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return getJobLogView();
    }

    @Override
    protected DBDDataFilter getEditorDataFilter() {
        OracleSchedulerJob job = getDatabaseObject();
        OracleTableBase logView = getJobLogView();
        if (logView == null) {
            return null;
        }
        List<DBDAttributeConstraint> constraints = new ArrayList<>();
        try {
            DBRProgressMonitor monitor = new VoidProgressMonitor();
            OracleTableColumn ownerAttr = logView.getAttribute(monitor, "OWNER");
            if (ownerAttr != null) {
                DBDAttributeConstraint ac = new DBDAttributeConstraint(ownerAttr, ownerAttr.getOrdinalPosition());
                ac.setVisible(false);
                ac.setOperator(DBCLogicalOperator.EQUALS);
                ac.setValue(job.getOwner());
                constraints.add(ac);
            }
            OracleTableColumn jobNameAttr = logView.getAttribute(monitor, "JOB_NAME");
            if (jobNameAttr != null) {
                DBDAttributeConstraint ac = new DBDAttributeConstraint(jobNameAttr, jobNameAttr.getOrdinalPosition());
                ac.setVisible(false);
                ac.setOperator(DBCLogicalOperator.EQUALS);
                ac.setValue(job.getName());
                constraints.add(ac);
            }
            OracleTableColumn logDateAttr = logView.getAttribute(monitor, "LOG_DATE");
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

    private OracleTableBase getJobLogView() {
        DBRProgressMonitor monitor = new VoidProgressMonitor();

        try {
            OracleDataSource dataSource = getDatabaseObject().getDataSource();
            OracleSchema systemSchema = dataSource.getSchema(monitor, OracleConstants.SCHEMA_SYS);
            if (systemSchema != null) {
                return systemSchema.getView(monitor, LOG_VIEW_NAME);
            }
            return null;
        } catch (DBException e) {
            log.error("Can't find log table", e);
            return null;
        }
    }
}
