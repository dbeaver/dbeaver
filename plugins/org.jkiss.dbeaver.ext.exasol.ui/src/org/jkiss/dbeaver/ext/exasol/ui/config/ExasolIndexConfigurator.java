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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndex;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;

import java.util.Arrays;
import java.util.Map;


public class ExasolIndexConfigurator implements DBEObjectConfigurator<ExasolTableIndex> {


	@Override
	public ExasolTableIndex configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull ExasolTableIndex index, @NotNull Map<String, Object> options) {
		return UITask.run(() -> {
				EditIndexPage editPage = new EditIndexPage(
						"create index",
						index,
						Arrays.asList(new DBSIndexType("LOCAL","LOCAL"), new DBSIndexType("GLOBAL","GLOBAL")),
						false
					);
				if (!editPage.edit()) {
					return null;
				}
				
				index.setIndexType(editPage.getIndexType());
				int colIndex = 1;
				for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
					index.addColumn(
								new ExasolTableIndexColumn(index, (ExasolTableColumn) tableColumn, colIndex++)
							);
				}
				index.setName(index.getIndexType().getName() + " INDEX " + index.getSimpleColumnString());
				return index;
			});
		}


}
