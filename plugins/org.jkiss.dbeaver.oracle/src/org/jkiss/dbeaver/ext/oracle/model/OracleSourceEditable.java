/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * Stored code interface
 */
public interface OracleSourceEditable extends OracleSourceObject, DBSObjectStateful {

    boolean isSourceValid();

    void setSourceValid(boolean valid);

    String getCompileQuery();

}
