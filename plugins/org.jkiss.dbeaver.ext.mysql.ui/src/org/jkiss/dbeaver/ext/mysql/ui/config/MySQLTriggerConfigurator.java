/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

/**
 * MySQL sequence configurator
 */
public class MySQLTriggerConfigurator implements DBEObjectConfigurator<MySQLTrigger> {
    
    @Override
    public MySQLTrigger configureObject(DBRProgressMonitor monitor, Object parent, MySQLTrigger trigger) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(trigger.getDataSource(), DBSEntityType.TRIGGER);
            if (!editPage.edit()) {
                return null;
            }
            trigger.setName(editPage.getEntityName());
            //trigger.setManipulationType(editPage.getM);
            trigger.setObjectDefinitionText(
                "CREATE TRIGGER " + DBUtils.getQuotedIdentifier(trigger) + "\n" +
                    trigger.getActionTiming() + " " + trigger.getManipulationType() + "\n" +
                    "ON " + DBUtils.getQuotedIdentifier(trigger.getParentObject()) + " FOR EACH ROW\n");
            return trigger;
        });
    }

}
