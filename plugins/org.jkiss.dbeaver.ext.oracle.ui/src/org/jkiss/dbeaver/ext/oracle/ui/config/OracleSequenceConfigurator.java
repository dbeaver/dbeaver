package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.jkiss.dbeaver.ext.oracle.model.OracleSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.math.BigDecimal;

public class OracleSequenceConfigurator implements DBEObjectConfigurator<OracleSequence> {

    @Override
    public OracleSequence configureObject(DBRProgressMonitor monitor, Object container, OracleSequence sequence) {
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
