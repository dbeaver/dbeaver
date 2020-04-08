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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraint;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraintColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * Postgre constraint configurator
 */
public class PostgreConstraintConfigurator implements DBEObjectConfigurator<PostgreTableConstraint> {


    @Override
    public PostgreTableConstraint configureObject(DBRProgressMonitor monitor, Object parent, PostgreTableConstraint constraint) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
                PostgreMessages.edit_constraint_page_add_constraint,
                constraint,
                new DBSEntityConstraintType[] {
                    DBSEntityConstraintType.PRIMARY_KEY,
                    DBSEntityConstraintType.UNIQUE_KEY,
                    DBSEntityConstraintType.CHECK });
            if (!editPage.edit()) {
                return null;
            }

            constraint.setName(editPage.getConstraintName());
            constraint.setConstraintType(editPage.getConstraintType());
            if (constraint.getConstraintType().isCustom()) {
                constraint.setSource(editPage.getConstraintExpression());
            } else {
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    constraint.addColumn(
                        new PostgreTableConstraintColumn(
                            constraint,
                            (PostgreAttribute) tableColumn,
                            colIndex++));
                }
            }
            return constraint;
        });
    }
}
