/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017-2017 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

public class ExasolCreateForeignKeyDialog extends EditForeignKeyPage {
    public ExasolCreateForeignKeyDialog(String title, ExasolTableForeignKey foreignKey) {
        super(title, foreignKey, new DBSForeignKeyModifyRule[0]);
    }

    @Override
    protected boolean supportsCustomName() {
        return true;
    }

}
