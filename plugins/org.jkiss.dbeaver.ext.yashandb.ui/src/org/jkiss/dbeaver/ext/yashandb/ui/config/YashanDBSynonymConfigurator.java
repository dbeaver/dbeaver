package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSynonym;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

public class YashanDBSynonymConfigurator implements DBEObjectConfigurator<YashanDBSynonym> {

    @Override
    public YashanDBSynonym configureObject(DBRProgressMonitor monitor, Object container, YashanDBSynonym synonym, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(synonym.getDataSource(), DBSEntityType.SYNONYM);
            if (!page.edit()) {
                return null;
            }

            synonym.setName(page.getEntityName());

            return synonym;
        });
    }

}
