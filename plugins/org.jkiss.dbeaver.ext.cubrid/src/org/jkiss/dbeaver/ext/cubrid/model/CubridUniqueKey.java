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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class CubridUniqueKey extends GenericUniqueKey {

    public CubridUniqueKey(
            GenericTableBase table,
            String name,
            String remarks,
            DBSEntityConstraintType constraintType,
            boolean persisted) {
        super(table, name, remarks, constraintType, persisted);
    }

    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    public void setName(String name) {
        super.setName(name != null ? name.toLowerCase() : null);
    }

}
