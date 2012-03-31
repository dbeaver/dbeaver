/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * Abstract tunnel
 */
public interface DBWTunnel extends DBWNetworkHandler {

    DBPConnectionInfo initializeTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, DBPConnectionInfo connectionInfo)
        throws DBException, IOException;

    void closeTunnel(DBRProgressMonitor monitor, DBPConnectionInfo connectionInfo)
        throws DBException, IOException;

}
