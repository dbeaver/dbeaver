/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

/**
 * Process controller
 */
public interface DBRProcessController {

    DBRProcessDescriptor getProcessDescriptor();

    void terminateProcess();

}
