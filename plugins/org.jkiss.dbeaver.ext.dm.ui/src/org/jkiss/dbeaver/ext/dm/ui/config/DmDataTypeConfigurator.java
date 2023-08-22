package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.dm.model.DmDataType;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class DmDataTypeConfigurator implements DBEObjectConfigurator<DmDataType> {

	@Override
	public DmDataType configureObject(DBRProgressMonitor monitor, Object container, DmDataType dataType,Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(dataType.getDataSource(), DBSEntityType.TYPE);
            if (!editPage.edit()) {
                return null;
            }
            dataType.setName(editPage.getEntityName());
            dataType.setObjectDefinitionText("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
                "(\n" + //$NON-NLS-1$
                ")"); //$NON-NLS-1$
            return dataType;
        });
	}

}
