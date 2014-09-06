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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * postgresqlDataSource
 */
public class PostgreMetaModel extends GenericMetaModel
{
    static final Log log = LogFactory.getLog(PostgreMetaModel.class);

    public PostgreMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    protected GenericProcedure createProcedureImpl(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String remarks,
        DBSProcedureType procedureType)
    {
        return new PostgreProcedure(
            container,
            procedureName,
            specificName,
            remarks,
            procedureType);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return PostgreUtils.getViewSource(monitor, sourceObject);
    }

}
