package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDBLink;
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
 * @Date 2023/7/20 10:27
 */
public class YashanDBDBLinkConfigurator implements DBEObjectConfigurator<YashanDBDBLink> {
    @Override
    public YashanDBDBLink configureObject(DBRProgressMonitor monitor, Object container, YashanDBDBLink dbLink, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(dbLink.getDataSource(), DBSEntityType.DBLINK);
            if (!page.edit()) {
                return null;
            }

            dbLink.setName(page.getEntityName());
            return dbLink;
        });
    }
}

