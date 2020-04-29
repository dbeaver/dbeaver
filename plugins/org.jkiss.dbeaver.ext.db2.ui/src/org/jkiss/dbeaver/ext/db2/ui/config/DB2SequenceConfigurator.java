package org.jkiss.dbeaver.ext.db2.ui.config;

import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class DB2SequenceConfigurator implements DBEObjectConfigurator<DB2Sequence> {

    @Override
    public DB2Sequence configureObject(DBRProgressMonitor monitor, Object container, DB2Sequence sequence) {
    	DB2Schema schema = (DB2Schema) container;
        return new UITask<DB2Sequence>() {
            @Override
            protected DB2Sequence runTask() {
                EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
                if (!page.edit()) {
                    return null;
                }

                return new DB2Sequence(schema, page.getEntityName());
            }
        }.execute();
    }

}
