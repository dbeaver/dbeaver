/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import java.util.List;

/**
 * Transaction abstraction
 */
public interface DBCTransaction {

    List<DBCSavepoint> getSavepoints();

}
