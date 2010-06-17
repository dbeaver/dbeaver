/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import java.util.List;

/**
 * Transaction abstraction
 */
public interface DBCTransaction {

    List<DBCSavepoint> getSavepoints();

}
