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

import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * FireBirdTrigger
 */
public abstract class FireBirdTrigger<OWNER extends DBSObject> extends GenericTrigger implements DBPSystemObject {

    private final FireBirdTriggerType type;
    private final int sequence;
    private final boolean isSystem;

    public FireBirdTrigger(OWNER container, String name, String description, FireBirdTriggerType type, int sequence, boolean isSystem) {
        super(container, name, description);

        this.type = type;
        this.sequence = sequence;
        this.isSystem = isSystem;
    }

    public FireBirdTriggerType getType() {
        return type;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 10)
    public String getTriggerType() {
        return type.getDisplayName();
    }

    @Property(viewable = true, editable = true, updatable = false, order = 11)
    public int getSequence() {
        return sequence;
    }

    @Override
    public boolean isSystem() {
        return isSystem;
    }
}
