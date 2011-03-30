/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.edit.DBEObjectCommanderImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.List;

/**
 * Object commander.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBECommandQueue<OBJECT_TYPE extends DBPObject> extends Collection<DBECommand<OBJECT_TYPE>> {

    OBJECT_TYPE getObject();

    DBECommandQueue getParentQueue();

    Collection<DBECommandQueue> getSubQueues();

}