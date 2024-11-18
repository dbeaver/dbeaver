/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.kingbase.ui.config;

import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseAttribute;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableConstraint;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableConstraintColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * Kingbase constraint configurator
 */
public class KingbaseConstraintConfigurator implements DBEObjectConfigurator<KingbaseTableConstraint> {


    @Override
    public KingbaseTableConstraint configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent, @NotNull KingbaseTableConstraint constraint, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            EditConstraintPage editPage = new EditConstraintPage(
            	KingbaseMessages.edit_constraint_page_add_constraint,
                constraint);
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
                        new KingbaseTableConstraintColumn(
                            constraint,
                            (KingbaseAttribute) tableColumn,
                            colIndex++));
                }
            }
            return constraint;
        });
    }
}
