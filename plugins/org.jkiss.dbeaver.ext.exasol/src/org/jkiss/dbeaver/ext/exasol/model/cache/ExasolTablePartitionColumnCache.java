/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2019 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.model.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTablePartitionColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public class ExasolTablePartitionColumnCache extends AbstractObjectCache<ExasolTable, ExasolTablePartitionColumn> {

	
    private List<ExasolTablePartitionColumn> tablePartitionColumns;

    public ExasolTablePartitionColumnCache() {
    	tablePartitionColumns = new ArrayList<ExasolTablePartitionColumn>();
	}
    
	@Override
	public Collection<ExasolTablePartitionColumn> getAllObjects(DBRProgressMonitor monitor, ExasolTable owner)
			throws DBException {
		if (tablePartitionColumns.isEmpty() && ! super.fullCache)
		{
	    	for( ExasolTableColumn col: owner.getAttributes(new VoidProgressMonitor()))
			{
				if (col.getPartitionKeyOrdinalPosition() != null)
				{
					tablePartitionColumns.add(new ExasolTablePartitionColumn(owner, col, col.getPartitionKeyOrdinalPosition().intValue()));
				}
			}
			sortPartitionColumns();
			super.setCache(tablePartitionColumns);
		}
		return tablePartitionColumns;
	}
	
	@Override
	public void clearCache() {
		super.clearCache();
		tablePartitionColumns.clear();
	}

	@Override
	public ExasolTablePartitionColumn getObject(DBRProgressMonitor monitor, ExasolTable owner, String name)
			throws DBException {
		if (!super.isFullyCached())
		{
			getAllObjects(new VoidProgressMonitor(), owner);
		}
		if (tablePartitionColumns.stream()
				.filter(o -> o.getTableColumn().getName().equals(name)).findFirst().isPresent())
		{
			return tablePartitionColumns.stream()
			.filter(o -> o.getName().equals(name)).findFirst().get();
		}
		return null;
	}
	
    private void sortPartitionColumns()
    {
    	tablePartitionColumns = tablePartitionColumns.stream()
    			.sorted(Comparator.comparing(ExasolTablePartitionColumn::getOrdinalPosition))
    			.collect(Collectors.toCollection(ArrayList::new));
    }

	public Collection<ExasolTableColumn> getAvailableTableColumns(ExasolTable owner) throws DBException {
		List<ExasolTableColumn> cols = new ArrayList<ExasolTableColumn>();
		
		cols = owner.getAttributes(new VoidProgressMonitor()).stream()
				.filter(c -> ! tablePartitionColumns.stream()
						.filter(pc -> pc.getTableColumn() != null && pc.getName().equals(c.getName()))
						.findFirst().isPresent()
				)
				.filter(c -> c.getDataKind() == DBPDataKind.DATETIME || c.getDataKind() == DBPDataKind.NUMERIC )
				.collect(Collectors.toList());
		
		return cols;
	}


}
