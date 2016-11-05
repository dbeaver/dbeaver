/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.tools.maintenance;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

public class ExasolImportTableTool implements IExternalTool {

	public ExasolImportTableTool()
	{
	}
	
	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart,
			Collection<DBSObject> objects) throws DBException
	{
		List<ExasolTable> tables = CommonUtils.filterCollection(objects, ExasolTable.class);
		List<ExasolSchema> schemas = CommonUtils.filterCollection(objects, ExasolSchema.class);
		
		//add tables for all Schemas but ignore views in schema
		for(ExasolSchema schema : schemas)
		{
			tables.addAll(schema.getTables(VoidProgressMonitor.INSTANCE));
		}
		
		// create TableBase Objects list
		@SuppressWarnings({ "unchecked", "rawtypes" })
		HashSet<ExasolTableBase> tableBaseObjects = new HashSet();
		
		//add tables
		for(ExasolTable table : tables)
		{
			tableBaseObjects.add((ExasolTableBase) table);
		}
		
		
		if (!tableBaseObjects.isEmpty()) {
			ExasolImportTableToolDialog dialog = new ExasolImportTableToolDialog(activePart.getSite(), tableBaseObjects) ;
			dialog.open();
		}

	}
}
