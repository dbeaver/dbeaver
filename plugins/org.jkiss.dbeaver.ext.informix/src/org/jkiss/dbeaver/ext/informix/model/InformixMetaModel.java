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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.informix.InformixUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * InformixDataSource
 */
public class InformixMetaModel extends GenericMetaModel
{
    static final Log log = Log.getLog(InformixMetaModel.class);

    public InformixMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return InformixUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return InformixUtils.getProcedureSource(monitor, sourceObject);
    }
    
    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
    	String tableDDL = super.getTableDDL(monitor, sourceObject);
    	// Triggers, Serials
    	// 
    	return tableDDL + InformixUtils.getTriggerDDL(monitor, sourceObject);
    }
    
}