/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Database structure listener
 */
public interface DBSListener {

    void handleObjectEvent(DBSObjectAction action,  DBSObject object);

}
