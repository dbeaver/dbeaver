package org.jkiss.dbeaver.ext.gaussdb.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBPackage;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class GaussDBPackageConfigurator implements DBEObjectConfigurator<GaussDBPackage> {

    public static final DBSEntityType PACKAGE = new DBSEntityType("package", "Package", DBIcon.TREE_PACKAGE, true);

    @Override
    public GaussDBPackage configureObject(DBRProgressMonitor monitor,
                                          DBECommandContext commandContext,
                                          Object container,
                                          GaussDBPackage gaussdbPackage,
                                          Map<String, Object> options) {
        return new UITask<GaussDBPackage>() {
            @Override
            protected GaussDBPackage runTask() throws DBException {
                EntityEditPage editPage = new EntityEditPage(gaussdbPackage.getDataSource(), PACKAGE);
                if (!editPage.edit()) {
                    return null;
                }
                String packName = editPage.getEntityName();
                gaussdbPackage.setName(packName);
                gaussdbPackage.setObjectDefinitionText("CREATE OR REPLACE PACKAGE " + packName + "\n" + "AS\n"
                            + "-- Package header\n" + "END " + packName + ";");
                gaussdbPackage.setExtendedDefinitionText("CREATE OR REPLACE PACKAGE BODY " + packName + "\n" + "AS\n"
                            + "-- Package body\n" + "END " + packName + ";");
                return gaussdbPackage;
            }
        }.execute();
    }
}