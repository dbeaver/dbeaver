package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataType;
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
public class YashanDBDataTypeConfigurator implements DBEObjectConfigurator<YashanDBDataType> {
    @Override
    public YashanDBDataType configureObject(DBRProgressMonitor monitor, Object parent, YashanDBDataType dataType, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(dataType.getDataSource(), DBSEntityType.TYPE);
            if (!editPage.edit()) {
                return null;
            }
            dataType.setName(editPage.getEntityName());
            dataType.setObjectDefinitionText("TYPE " + dataType.getName() + " AS OBJECT\n" +
                    "(\n" +
                    ")");
            return dataType;
        });
    }
}
