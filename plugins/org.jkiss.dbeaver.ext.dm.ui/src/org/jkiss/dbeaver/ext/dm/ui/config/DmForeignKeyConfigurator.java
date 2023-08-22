package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
import org.jkiss.dbeaver.ext.dm.model.DmTableConstraint;
import org.jkiss.dbeaver.ext.dm.model.DmTableForeignKey;
import org.jkiss.dbeaver.ext.dm.model.DmTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.dm.ui.internal.DmUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

public class DmForeignKeyConfigurator implements DBEObjectConfigurator<DmTableForeignKey> {

	@Override
	public DmTableForeignKey configureObject(DBRProgressMonitor monitor, Object container, DmTableForeignKey foreignKey,Map<String, Object> options) {
        return UITask.run(() -> {
            EditForeignKeyPage editPage = new EditForeignKeyPage(
                DmUIMessages.edit_dm_foreign_key_manager_dialog_title,
                foreignKey,
                new DBSForeignKeyModifyRule[]{
                    DBSForeignKeyModifyRule.NO_ACTION,
                    DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                    DBSForeignKeyModifyRule.SET_NULL,
                    DBSForeignKeyModifyRule.SET_DEFAULT},options);
            editPage.setSupportsCustomName(true);
            if (!editPage.edit()) {
                return null;
            }

            foreignKey.setReferencedConstraint((DmTableConstraint) editPage.getUniqueConstraint());
            foreignKey.setName(editPage.getName());
            foreignKey.setDeleteRule(editPage.getOnDeleteRule());
            int colIndex = 1;
            for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                foreignKey.addColumn(
                    new DmTableForeignKeyColumn(
                        foreignKey,
                        (DmTableColumn) tableColumn.getOwnColumn(),
                        colIndex++));
            }
            return foreignKey;
        });
	}

}
