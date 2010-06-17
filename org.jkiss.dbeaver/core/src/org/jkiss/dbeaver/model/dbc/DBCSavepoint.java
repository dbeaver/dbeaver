/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

/**
 * Transaction savepoint
 */
public interface DBCSavepoint {

    int getSavepointId();

    String getSavepointName();

    DBCExecutionContext getContext();

}
