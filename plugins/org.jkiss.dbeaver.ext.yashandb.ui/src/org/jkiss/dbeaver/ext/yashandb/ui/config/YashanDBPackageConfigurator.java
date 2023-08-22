package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBPackage;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBPackageConfigurator implements DBEObjectConfigurator<YashanDBPackage> {

    @Override
    public YashanDBPackage configureObject(DBRProgressMonitor monitor, Object container, YashanDBPackage yashandbPackage, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(yashandbPackage.getDataSource(), DBSEntityType.PACKAGE);
            if (!editPage.edit()) {
                return null;
            }
            String packName = editPage.getEntityName();
            yashandbPackage.setName(packName);
            yashandbPackage.setObjectDefinitionText(
                    "CREATE OR REPLACE PACKAGE " + packName + "\n" +
                            "AS\n" +
                            "-- Package header\n" +
                            "END " + packName + ";");
            yashandbPackage.setExtendedDefinitionText(
                    "CREATE OR REPLACE PACKAGE BODY " + packName + "\n" +
                            "AS\n" +
                            "-- Package body\n" +
                            "END " + packName + ";");
            return yashandbPackage;
        });
    }
}
