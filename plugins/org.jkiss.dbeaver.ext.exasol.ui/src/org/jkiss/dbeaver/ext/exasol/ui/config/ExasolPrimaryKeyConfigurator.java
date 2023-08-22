/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableKeyColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableUniqueKey;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExasolPrimaryKeyConfigurator implements DBEObjectConfigurator<ExasolTableUniqueKey> {
    protected static final Log log = Log.getLog(ExasolPrimaryKeyConfigurator.class);

    @Override
    public ExasolTableUniqueKey configureObject(DBRProgressMonitor monitor, Object container, ExasolTableUniqueKey constraint, Map<String, Object> options) {
        return new UITask<ExasolTableUniqueKey>() {
            @Override
            protected ExasolTableUniqueKey runTask()
            {
                EditConstraintPage editPage = new EditConstraintPage(
                        ExasolMessages.edit_exasol_constraint_manager_dialog_title,
                        constraint,
                        new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY });
                if (!editPage.edit()) {
                    return null;
                }

                constraint.setConstraintType(editPage.getConstraintType());
                constraint.setEnabled(editPage.isEnableConstraint());
                constraint.setName(editPage.getConstraintName());

                List<ExasolTableKeyColumn> constColumns = new ArrayList<ExasolTableKeyColumn>();
                int ordinalPosition = 0;
                for(DBSEntityAttribute tableColumn  : editPage.getSelectedAttributes())
                {
                    ExasolTableKeyColumn col;
                    try {
                        col = new ExasolTableKeyColumn(constraint, constraint.getTable().getAttribute(monitor, tableColumn.getName()), ++ordinalPosition);
                    } catch (DBException e) {
                        log.error("Could not find column " + tableColumn.getName() + " in table " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL));
                        return null;
                    }
                    constColumns.add(col);
                }
                constraint.setColumns(constColumns);
                return constraint;
            }
        }.execute();
    }
}
