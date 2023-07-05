package org.jkiss.dbeaver.ext.altibase.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class AltibaseSequenceConfigurator implements DBEObjectConfigurator<AltibaseSequence> {

    @Override
    public AltibaseSequence configureObject(DBRProgressMonitor monitor, Object container, 
            AltibaseSequence sequence, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(sequence.getDataSource(), DBSEntityType.SEQUENCE);
            if (!page.edit()) {
                return null;
            }

            sequence.setName(page.getEntityName());

            return sequence;
        });
    }

}