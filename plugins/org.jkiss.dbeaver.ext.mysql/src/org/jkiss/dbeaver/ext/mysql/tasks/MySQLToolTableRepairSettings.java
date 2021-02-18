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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class MySQLToolTableRepairSettings extends SQLToolExecuteSettings<MySQLTableBase> {
    private boolean isQuick;
    private boolean isExtended;
    private boolean useFRM;

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isQuick() {
        return isQuick;
    }

    public void setQuick(boolean quick) {
        isQuick = quick;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isUseFRM() {
        return useFRM;
    }

    public void setUseFRM(boolean useFRM) {
        this.useFRM = useFRM;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        isQuick = JSONUtils.getBoolean(config, "quick");
        isExtended = JSONUtils.getBoolean(config, "extended");
        useFRM = JSONUtils.getBoolean(config, "use_frm");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("quick", isQuick);
        config.put("extended", isExtended);
        config.put("use_frm", useFRM);
    }
}
