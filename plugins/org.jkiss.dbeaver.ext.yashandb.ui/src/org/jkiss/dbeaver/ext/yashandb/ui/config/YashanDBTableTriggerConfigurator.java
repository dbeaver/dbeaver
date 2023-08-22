package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableTrigger;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/09
 * @Description:
 */
public class YashanDBTableTriggerConfigurator implements DBEObjectConfigurator<YashanDBTableTrigger> {
    @Override
    public YashanDBTableTrigger configureObject(DBRProgressMonitor monitor, Object container, YashanDBTableTrigger newTrigger, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(newTrigger.getDataSource(), DBSEntityType.TRIGGER);
            if (!editPage.edit()) {
                return null;
            }
            newTrigger.setName(editPage.getEntityName());
            newTrigger.setObjectDefinitionText("CREATE OR REPLACE TRIGGER " + editPage.getEntityName() + "\n" +
                    "BEGIN\n" +
                    "END;");
            return newTrigger;
        });
    }
}
