/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * FireBirdDataSource
 */
public class FireBirdTrigger extends GenericTrigger {

    private FireBirdTriggerType type;
    private int sequence;

    public FireBirdTrigger(GenericStructContainer container, GenericTable table, String name, String description, FireBirdTriggerType type, int sequence) {
        super(container, table, name, description);

        this.type = type;
        this.sequence = sequence;
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
}
