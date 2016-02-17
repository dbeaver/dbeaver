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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

/**
 * Constraint column
 */
public class DBVEntityConstraintColumn implements DBSEntityAttributeRef {

    private final DBVEntityConstraint constraint;
    private final String attributeName;

    public DBVEntityConstraintColumn(DBVEntityConstraint constraint, String attributeName)
    {
        this.constraint = constraint;
        this.attributeName = attributeName;
    }

    public DBVEntityConstraintColumn(DBVEntityConstraint constraint, DBVEntityConstraintColumn copy) {
        this.constraint = constraint;
        this.attributeName = copy.attributeName;
    }

    @NotNull
    @Override
    public DBSEntityAttribute getAttribute()
    {
        // Here we use void monitor.
        // In real life entity columns SHOULD be already read so it doesn't matter
        // But I'm afraid that in some very special cases it does. Thant's too bad.
        return constraint.getEntity().getAttribute(VoidProgressMonitor.INSTANCE, attributeName);
    }

    public String getAttributeName()
    {
        return attributeName;
    }
}
