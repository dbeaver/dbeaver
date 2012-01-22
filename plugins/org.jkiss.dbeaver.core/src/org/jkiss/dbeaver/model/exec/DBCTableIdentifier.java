/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.struct.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.List;

/**
 * Result set table metadata
 */
public interface DBCTableIdentifier {

    /**
     * Table unique constraint
     * @return constraint
     */
    DBSTableConstraint getConstraint();

    /**
     * Table unique index
     * @return constraint
     */
    DBSTableIndex getIndex();

    /**
     * Result set columns
     * @return list of result set columns.
     */
    List<? extends DBCColumnMetaData> getResultSetColumns();

    List<? extends DBSTableColumn> getTableColumns();

}