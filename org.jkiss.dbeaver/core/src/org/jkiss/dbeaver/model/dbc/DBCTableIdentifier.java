/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.struct.DBSConstraint;

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
     * Result set columns
     * @return list of result set columns.
     */
    List<? extends DBCColumnMetaData> getResultSetColumns();

}