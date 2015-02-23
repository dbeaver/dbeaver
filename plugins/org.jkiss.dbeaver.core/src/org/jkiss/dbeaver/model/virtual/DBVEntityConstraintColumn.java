/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

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

    @NotNull
    @Override
    public DBSEntityAttribute getAttribute()
    {
        // Here we use void monitor.
        // In real life entity columns SHOULD be already read so it doesn't matter
        // But I'm afraid that in some very special cases it does. Thant's too bad.
        return constraint.getParentObject().getAttribute(VoidProgressMonitor.INSTANCE, attributeName);
    }

    public String getAttributeName()
    {
        return attributeName;
    }
}
