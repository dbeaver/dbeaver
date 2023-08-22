package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBConstants;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchedulerJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/5 11:56
 */
public class SchedulerJobActionEditor extends SQLSourceViewer<YashanDBSchedulerJob> {

    @Override
    protected String getCompileCommandId()
    {
        return YashanDBConstants.CMD_COMPILE;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        YashanDBSchedulerJob schedulerJob = getSourceObject();
        return schedulerJob.getJobAction();
//        return ((DBPScriptObjectExt)getSourceObject()).getExtendedDefinitionText(monitor);
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
                monitor,
                YashanDBConstants.PROP_OBJECT_BODY_DEFINITION,
                sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }
}

