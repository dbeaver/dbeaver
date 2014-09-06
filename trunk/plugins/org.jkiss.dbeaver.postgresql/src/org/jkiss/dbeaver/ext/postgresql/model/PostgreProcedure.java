/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * PostgreProcedure
 */
public class PostgreProcedure extends GenericProcedure
{

    private String sourceDeclaration;

    public PostgreProcedure(GenericStructContainer container, String procedureName, String specificName, String description, DBSProcedureType procedureType) {
        super(container, procedureName, specificName, description, procedureType);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getSource(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = PostgreUtils.getProcedureSource(monitor, this);
        }
        return sourceDeclaration;
    }

}
