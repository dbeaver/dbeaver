/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Object state
 */
public class DBSObjectState
{
    public static final DBSObjectState NORMAL = new DBSObjectState();
    public static final DBSObjectState INVALID = new DBSObjectState();
    public static final DBSObjectState ACTIVE = new DBSObjectState();
    public static final DBSObjectState UNKNOWN = new DBSObjectState();
}