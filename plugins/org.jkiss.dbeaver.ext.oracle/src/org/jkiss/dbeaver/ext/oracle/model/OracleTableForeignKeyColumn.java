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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.List;

/**
 * GenericConstraintColumn
 */
public class OracleTableForeignKeyColumn extends OracleTableConstraintColumn implements DBSTableForeignKeyColumn
{

    public OracleTableForeignKeyColumn(
        OracleTableForeignKey constraint,
        OracleTableColumn tableColumn,
        int ordinalPosition)
    {
        super(constraint, tableColumn, ordinalPosition);
    }

    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public OracleTableColumn getReferencedColumn()
    {
        OracleTableConstraint referencedConstraint = ((OracleTableForeignKey) getParentObject()).getReferencedConstraint();
        if (referencedConstraint != null) {
            List<OracleTableConstraintColumn> ar = referencedConstraint.getAttributeReferences(VoidProgressMonitor.INSTANCE);
            if (ar != null) {
                return ar.get(getOrdinalPosition() - 1).getAttribute();
            }
        }
        return null;
    }

}
