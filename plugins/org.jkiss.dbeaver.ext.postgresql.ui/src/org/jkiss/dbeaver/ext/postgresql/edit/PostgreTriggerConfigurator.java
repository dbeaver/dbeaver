/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableReal;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

/**
 * Postgre sequence configurator
 */
public class PostgreTriggerConfigurator implements DBEObjectConfigurator<PostgreTableReal, PostgreTrigger> {
    @Override
    public PostgreTrigger configureObject(DBRProgressMonitor monitor, PostgreTableReal parent, PostgreTrigger trigger) {
	return new UITask<PostgreTrigger>() {
	    @Override
	    protected PostgreTrigger runTask() {
	        EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.TRIGGER);
		if (!editPage.edit()) {
		    return null;
		}
		trigger.setName(editPage.getEntityName());
		return trigger;
	    }
	}.execute();
    }

}
