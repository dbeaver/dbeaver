/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableColumn;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableUniqueKey;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableUniqueKeyColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * SQL server unique constraint configurator
 */
public class SQLServerUniqueKeyConfigurator implements DBEObjectConfigurator<SQLServerTableUniqueKey> {

    @Override
    public SQLServerTableUniqueKey configureObject(DBRProgressMonitor monitor, Object container, SQLServerTableUniqueKey primaryKey) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
                "Create constraint",
                primaryKey,
                new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY, DBSEntityConstraintType.UNIQUE_KEY} );
            if (!editPage.edit()) {
                return null;
            }
            primaryKey.setConstraintType(editPage.getConstraintType());
            primaryKey.setName(editPage.getConstraintName());
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                primaryKey.addColumn(
                    new SQLServerTableUniqueKeyColumn(
                        primaryKey,
                        (SQLServerTableColumn) tableColumn,
                        colIndex++));
            }
            return primaryKey;
        });
    }

}
