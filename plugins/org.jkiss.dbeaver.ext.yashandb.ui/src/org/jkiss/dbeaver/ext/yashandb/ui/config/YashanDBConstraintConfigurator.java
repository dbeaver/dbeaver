/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.ui.config;

import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableConstraint;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableConstraintColumn;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

public class YashanDBConstraintConfigurator implements DBEObjectConfigurator<YashanDBTableConstraint> {

	@Override
	public YashanDBTableConstraint configureObject(@Nullable DBRProgressMonitor monitor,
			@Nullable DBECommandContext commandContext, @Nullable Object container,
			@Nullable YashanDBTableConstraint constraint, @Nullable Map<String, Object> options) {
		return UITask.run(() -> {
			EditConstraintPage editPage = new EditConstraintPage(
					YashanDBUIMessages.edit_yashandb_constraint_manager_dialog_title, constraint);
			if (!editPage.edit()) {
				return null;
			}
			constraint.setName(editPage.getConstraintName());
			constraint.setConstraintType(editPage.getConstraintType());
			constraint.setCondition(editPage.getConstraintExpression());

			int colIndex = 1;
			for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
				constraint.addColumn(
						new YashanDBTableConstraintColumn(constraint, (YashanDBTableColumn) tableColumn, colIndex++));
			}

			return constraint;
		});
	}
}
