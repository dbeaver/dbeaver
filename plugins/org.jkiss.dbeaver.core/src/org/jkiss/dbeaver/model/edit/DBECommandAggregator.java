/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * Command aggregator.
 * Only single such command allowed in command context.
 * Usually it is the first command (create new object)
 */
public interface DBECommandAggregator<OBJECT_TYPE extends DBPObject> extends DBECommand<OBJECT_TYPE> {

    boolean aggregateCommand(DBECommand<?> command);

    void resetAggregatedCommands();
}
