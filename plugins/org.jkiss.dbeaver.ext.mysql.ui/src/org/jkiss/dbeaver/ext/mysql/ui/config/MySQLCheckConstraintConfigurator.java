package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableCheckConstraintColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableCheckConstraint;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

public class MySQLCheckConstraintConfigurator implements DBEObjectConfigurator<MySQLTableCheckConstraint> {
    @Override
    public MySQLTableCheckConstraint configureObject(DBRProgressMonitor monitor, Object container, MySQLTableCheckConstraint checkConstraint) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
                    MySQLUIMessages.edit_constraint_manager_title,
                    checkConstraint,
                    new DBSEntityConstraintType[] {DBSEntityConstraintType.CHECK});
            if (!editPage.edit()) {
                return null;
            }

            checkConstraint.setName(editPage.getConstraintName());
            checkConstraint.setConstraintType(editPage.getConstraintType());
            checkConstraint.setClause(editPage.getConstraintExpression());
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                checkConstraint.addColumn(
                        new MySQLTableCheckConstraintColumn(
                                checkConstraint,
                                (MySQLTableColumn) tableColumn ));
            }
            return checkConstraint;
        });
    }
}
