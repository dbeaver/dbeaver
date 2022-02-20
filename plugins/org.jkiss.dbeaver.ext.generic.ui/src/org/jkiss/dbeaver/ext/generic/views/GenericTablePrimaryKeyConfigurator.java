/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.generic.views;

import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

import java.util.Map;

/**
 * Generic table primary key configurator
 */
public class GenericTablePrimaryKeyConfigurator implements DBEObjectConfigurator<GenericUniqueKey> {


    @Override
    public GenericUniqueKey configureObject(DBRProgressMonitor monitor, Object table, GenericUniqueKey primaryKey, Map<String, Object> options) {
        boolean isSupportCheckConstraint = primaryKey.getDataSource().getMetaModel().supportsCheckConstraints();
        return new UITask<GenericUniqueKey>() {
            @Override
            protected GenericUniqueKey runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    "Create unique constraint",
                    primaryKey,
                    isSupportCheckConstraint ?
                            new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY, DBSEntityConstraintType.UNIQUE_KEY, DBSEntityConstraintType.CHECK} :
                            new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY, DBSEntityConstraintType.UNIQUE_KEY} );
                if (!editPage.edit()) {
                    return null;
                }

                primaryKey.setConstraintType(editPage.getConstraintType());
                primaryKey.setName(editPage.getConstraintName());
                if (primaryKey instanceof DBSTableCheckConstraint) {
                    ((DBSTableCheckConstraint)primaryKey).setCheckConstraintDefinition(editPage.getConstraintExpression());
                }
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    primaryKey.addColumn(
                        new GenericTableConstraintColumn(
                            primaryKey,
                            (GenericTableColumn) tableColumn,
                            colIndex++));
                }
                return primaryKey;
            }
        }.execute();
    }
}
