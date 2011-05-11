/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericIndexColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.Collection;
import java.util.Collections;

/**
 * Generic index manager
 */
public class GenericIndexManager extends JDBCIndexManager<GenericIndex, GenericTable> {

    @Override
    protected Collection<DBSIndexType> getSupportedIndexTypes()
    {
        return Collections.singletonList(DBSIndexType.OTHER);
    }

    @Override
    protected GenericIndex createNewIndex(
        IWorkbenchWindow workbenchWindow,
        GenericTable parent,
        DBSIndexType indexType,
        Collection<DBSTableColumn> indexColumns,
        Object from)
    {
        final GenericIndex index = new GenericIndex(
            parent,
            false,
            null,
            null,
            indexType,
            false);
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSTableColumn tableColumn : indexColumns) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
            }
            index.addColumn(
                new GenericIndexColumn(
                    index,
                    (GenericTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        idxName.append("_IDX");
        index.setName(JDBCObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

}
