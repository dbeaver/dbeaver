/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSIndex;
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
    DBSConstraint getConstraint();

    /**
     * Table unique index
     * @return constraint
     */
    DBSIndex getIndex();

    /**
     * Result set columns
     * @return list of result set columns.
     */
    List<? extends DBCColumnMetaData> getResultSetColumns();

    List<? extends DBSTableColumn> getTableColumns();

}