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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableConstraint;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableForeignKey;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

import java.util.Map;

public class YashanDBForeignKeyConfigurator implements DBEObjectConfigurator<YashanDBTableForeignKey> {

	@Override
	public YashanDBTableForeignKey configureObject(@Nullable DBRProgressMonitor monitor,
			@Nullable DBECommandContext commandContext, @Nullable Object container,
			@Nullable YashanDBTableForeignKey foreignKey, @Nullable Map<String, Object> options) {
		return UITask.run(() -> {
			EditForeignKeyPage editPage = new EditForeignKeyPage(
					YashanDBUIMessages.edit_yashandb_foreign_key_manager_dialog_title, foreignKey,
					new DBSForeignKeyModifyRule[] { DBSForeignKeyModifyRule.NO_ACTION, DBSForeignKeyModifyRule.CASCADE,
							DBSForeignKeyModifyRule.RESTRICT, DBSForeignKeyModifyRule.SET_NULL,
							DBSForeignKeyModifyRule.SET_DEFAULT },
					options);
			editPage.setSupportsCustomName(true);
			if (!editPage.edit()) {
				return null;
			}

			foreignKey.setReferencedConstraint((YashanDBTableConstraint) editPage.getUniqueConstraint());
			foreignKey.setName(editPage.getName());
			foreignKey.setDeleteRule(editPage.getOnDeleteRule());
			int colIndex = 1;
			for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
				foreignKey.addColumn(new YashanDBTableForeignKeyColumn(foreignKey,
						(YashanDBTableColumn) tableColumn.getOwnColumn(), colIndex++));
			}
			return foreignKey;
		});
	}

}
