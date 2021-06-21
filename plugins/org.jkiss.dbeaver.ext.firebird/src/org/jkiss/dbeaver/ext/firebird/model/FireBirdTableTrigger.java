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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

public class FireBirdTableTrigger extends FireBirdTrigger<GenericTableBase> implements DBPSystemObject {

    public FireBirdTableTrigger(GenericTableBase container, String name, String description, FireBirdTriggerType type, int sequence, boolean isSystem) {
        super(container, name, description, type, sequence, isSystem);
    }

    @Override
    @Property(viewable = true, order = 4)
    public DBSTable getTable() {
        return (DBSTable) getParentObject();
    }
}
