/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * Compile unit
 */
public interface OracleCompileUnit extends DBSObjectStateful {

    IDatabasePersistAction[] getCompileActions();

}
