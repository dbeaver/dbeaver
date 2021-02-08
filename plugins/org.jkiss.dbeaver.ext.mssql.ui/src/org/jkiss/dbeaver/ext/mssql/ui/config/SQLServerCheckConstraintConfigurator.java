/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableCheckConstraint;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * SQL server unique constraint manager
 */
public class SQLServerCheckConstraintConfigurator implements DBEObjectConfigurator<SQLServerTableCheckConstraint> {

    @Override
    public SQLServerTableCheckConstraint configureObject(DBRProgressMonitor monitor, Object container, SQLServerTableCheckConstraint constraint) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
                "Create CHECK constraint",
                constraint,
                new DBSEntityConstraintType[] {DBSEntityConstraintType.CHECK} );
            if (!editPage.edit()) {
                return null;
            }

            return null;
/*
            final SQLServerTableUniqueKey primaryKey = new SQLServerTableUniqueKey(
                parent,
                null,
                null,
                editPage.getConstraintType(),
                false);
            primaryKey.setName(editPage.getConstraintName());
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                primaryKey.addColumn(
                    new SQLServerTableConstraintColumn(
                        primaryKey,
                        (SQLServerTableColumn) tableColumn,
                        colIndex++));
            }
            return primaryKey;
*/
        });
    }

}
