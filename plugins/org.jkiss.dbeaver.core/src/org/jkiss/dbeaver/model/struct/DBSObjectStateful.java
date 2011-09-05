/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Object with state.
 */
public interface DBSObjectStateful extends DBSObject
{

    DBSObjectState getObjectState();

}
