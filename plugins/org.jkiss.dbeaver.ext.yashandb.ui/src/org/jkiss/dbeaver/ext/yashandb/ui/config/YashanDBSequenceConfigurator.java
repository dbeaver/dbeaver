package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSequenceConfigurator implements DBEObjectConfigurator<YashanDBSequence> {
    @Override
    public YashanDBSequence configureObject(DBRProgressMonitor monitor, Object container, YashanDBSequence sequence, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(sequence.getDataSource(), DBSEntityType.SEQUENCE);
            if (!page.edit()) {
                return null;
            }

            sequence.setName(page.getEntityName());
            sequence.setIncrementBy(1L);
            sequence.setMinValue(new BigDecimal(0));
            sequence.setCycle(false);
            return sequence;
        });
    }
}
