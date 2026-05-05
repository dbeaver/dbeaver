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
package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseConstraint;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTableColumn;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTableIndex;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class AltibaseTableManager extends GenericTableManager {
    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
            AltibaseTableColumn.class,
            AltibaseConstraint.class,
            GenericTableForeignKey.class,
            AltibaseTableIndex.class
            );

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }
}
