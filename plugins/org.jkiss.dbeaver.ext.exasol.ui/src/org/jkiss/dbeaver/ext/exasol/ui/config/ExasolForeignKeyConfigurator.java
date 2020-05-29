package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableUniqueKey;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.ArrayList;
import java.util.List;

public class ExasolForeignKeyConfigurator implements DBEObjectConfigurator<ExasolTableForeignKey> {
    protected static final Log log = Log.getLog(ExasolForeignKeyConfigurator.class);

    @Override
    public ExasolTableForeignKey configureObject(DBRProgressMonitor monitor, Object container, ExasolTableForeignKey foreignKey) {
        ExasolTable table = (ExasolTable) container;
        return new UITask<ExasolTableForeignKey>() {
            @Override
            protected ExasolTableForeignKey runTask() {
                ExasolCreateForeignKeyDialog editPage = new ExasolCreateForeignKeyDialog("Create Foreign Key", foreignKey);

                if (!editPage.edit()) {
                    return null;
                }
                foreignKey.setName(editPage.getName());
                foreignKey.setReferencedConstraint((ExasolTableUniqueKey)editPage.getUniqueConstraint());
                foreignKey.setEnabled(editPage.isEnabled());

                List<ExasolTableForeignKeyColumn> columns = new ArrayList<>();
                int cnt = 0;
                for (ExasolCreateForeignKeyDialog.FKColumnInfo column : editPage.getColumns()) {
                    try {
                        ExasolTable refTable = foreignKey.getReferencedConstraint().getTable();
                        columns.add(new ExasolTableForeignKeyColumn(
                                foreignKey,
                                table.getAttribute(monitor, column.getOwnColumn().getName()),
                                refTable.getAttribute(monitor, column.getRefColumn().getName()),
                                ++cnt));
                    } catch (DBException e) {
                        log.error("Could not get Attribute Information from Table");
                        return null;
                    }
                }

                foreignKey.setColumns(columns);

                return foreignKey;
            }
        }.execute();
    }
}
