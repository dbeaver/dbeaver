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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.AttributeEditPage;

public class SQLServerColumnConfigurator implements DBEObjectConfigurator<SQLServerTableColumn> {
    @Override
    public SQLServerTableColumn configureObject(DBRProgressMonitor monitor, Object container, SQLServerTableColumn column) {
        return new UITask<SQLServerTableColumn>() {
            @Override
            protected SQLServerTableColumn runTask() {
                final AttributeEditPage page = new AttributeEditPage(null, column);
                if (!page.edit()) {
                    return null;
                }
                return column;
            }
        }.execute();
    }
}
