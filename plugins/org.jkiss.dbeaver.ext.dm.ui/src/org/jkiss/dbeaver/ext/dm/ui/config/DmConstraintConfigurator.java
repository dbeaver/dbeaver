package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
import org.jkiss.dbeaver.ext.dm.model.DmTableConstraint;
import org.jkiss.dbeaver.ext.dm.model.DmTableConstraintColumn;
import org.jkiss.dbeaver.ext.dm.ui.internal.DmUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

public class DmConstraintConfigurator implements DBEObjectConfigurator<DmTableConstraint>{

	@Override
	public DmTableConstraint configureObject(DBRProgressMonitor monitor, Object container, DmTableConstraint constraint,Map<String, Object> options) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
                DmUIMessages.edit_dm_constraint_manager_dialog_title,
                constraint,
                new DBSEntityConstraintType[] {
                    DBSEntityConstraintType.PRIMARY_KEY,
                    DBSEntityConstraintType.UNIQUE_KEY, 
                    DBSEntityConstraintType.CHECK});
            if (!editPage.edit()) {
                return null;
            }
            constraint.setName(editPage.getConstraintName());
            constraint.setConstraintType(editPage.getConstraintType());

            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                constraint.addColumn(
                    new DmTableConstraintColumn(
                        constraint,
                        (DmTableColumn) tableColumn,
                        colIndex++));
            }
            return constraint;
        });
	}

}
