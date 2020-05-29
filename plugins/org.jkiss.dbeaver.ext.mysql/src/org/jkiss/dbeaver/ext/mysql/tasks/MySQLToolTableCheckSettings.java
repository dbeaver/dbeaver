/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

/**
 * Table check settings
 */
public class MySQLToolTableCheckSettings extends SQLToolExecuteSettings<MySQLTableBase> {
    private String option;

    @Property(viewable = true, editable = true, updatable = true, listProvider = CheckOptionListProvider.class)
    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public static class CheckOptionListProvider implements IPropertyValueListProvider<MySQLToolTableCheckSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(MySQLToolTableCheckSettings object) {
            return new String[] {
                "",
                "FOR UPGRADE",
                "QUICK",
                "FAST",
                "MEDIUM",
                "EXTENDED",
                "CHANGED"
            };
        }
    }

}
