/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

/**
 * Transaction savepoint
 */
public interface DBCSavepoint {

    int getId();

    String getName();

    DBCExecutionContext getContext();

}
