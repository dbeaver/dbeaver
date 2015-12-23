/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * PostgreGenericTrigger
 */
@Deprecated
public class PostgreGenericTrigger extends GenericTrigger {

    private String manipulation;
    private String orientation;
    private String timing;
    private String source;

    public PostgreGenericTrigger(GenericStructContainer container, GenericTable table, String name, String description, String manipulation, String orientation, String timing, String statement) {
        super(container, table, name, description);
        this.manipulation = manipulation;
        this.orientation = orientation;
        this.timing = timing;
        this.source = statement;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 20)
    public String getTiming() {
        return timing;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 21)
    public String getManipulation() {
        return manipulation;
    }

    @Property(viewable = true, editable = true, updatable = false, order = 22)
    public String getOrientation() {
        return orientation;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return source;
    }

    public void addManipulation(String manipulation) {
        this.manipulation += " OR " + manipulation;
    }
}
