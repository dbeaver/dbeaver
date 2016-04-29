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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.List;

/**
 * PostgreTableInheritance
 */
public class PostgreTableInheritance extends PostgreTableConstraintBase implements DBSEntityAssociation
{
    private final PostgreTableBase superTable;
    private int sequenceNum;

    public PostgreTableInheritance(
        @NotNull PostgreTableBase table,
        @NotNull PostgreTableBase superTable,
        int sequenceNum,
        boolean persisted)
    {
        super(table, DBSEntityConstraintType.INHERITANCE);
        this.setName(table.getFullQualifiedName() + "->" + superTable.getFullQualifiedName());
        this.setPersisted(persisted);
        this.superTable = superTable;
        this.sequenceNum = sequenceNum;
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint() {
        return this;
    }

    @Override
    @Property(viewable = true)
    public PostgreTableBase getAssociatedEntity() {
        return this.superTable;
    }

    @Property(viewable = true)
    public int getSequenceNum() {
        return sequenceNum;
    }

    @Nullable
    @Override
    public List<PostgreTableForeignKeyColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    void cacheAttributes(DBRProgressMonitor monitor, List<? extends PostgreTableConstraintColumn> children, boolean secondPass) {

    }

}
