package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchedulerJob;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/24 17:04
 */
public class YashanDBSchedulerJobConfigurator implements DBEObjectConfigurator<YashanDBSchedulerJob> {
    @Override
    public YashanDBSchedulerJob configureObject(DBRProgressMonitor monitor, Object container, YashanDBSchedulerJob schedulerJob, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(schedulerJob.getDataSource(), DBSEntityType.SCHEDULER_JOB);
            if (!page.edit()) {
                return null;
            }
            String schedulerJobName=page.getEntityName();
            schedulerJob.setName(schedulerJobName);
            String definitionText=
                    " BEGIN\n" +
                    "\tdbms_scheduler.create_job(\n" +
                    "\t '"+schedulerJobName+"',\n" +
                    "\t--please config your scheduler job param here\n\n" +
                    "\t);\n" +
                    "END;";
            schedulerJob.setObjectDefinitionText(definitionText);
            return schedulerJob;
        });
    }
}
