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
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableTrigger;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class YashanDBTableTriggerConfigurator implements DBEObjectConfigurator<YashanDBTableTrigger> {

	@Override
	public YashanDBTableTrigger configureObject(@Nullable DBRProgressMonitor monitor,
			@Nullable DBECommandContext commandContext, @Nullable Object container,
			@Nullable YashanDBTableTrigger newTrigger, @Nullable Map<String, Object> options) {
		return UITask.run(() -> {
			EntityEditPage editPage = new EntityEditPage(newTrigger.getDataSource(), DBSEntityType.TRIGGER);
			if (!editPage.edit()) {
				return null;
			}
			newTrigger.setName(editPage.getEntityName());
			newTrigger.setObjectDefinitionText(
					"CREATE OR REPLACE TRIGGER " + editPage.getEntityName() + "\n" + "BEGIN\n" + "END;");
			return newTrigger;
		});
	}
}
