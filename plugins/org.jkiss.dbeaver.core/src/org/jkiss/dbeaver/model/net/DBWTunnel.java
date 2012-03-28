/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net;

import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * Abstract tunnel
 */
public interface DBWTunnel {

    void initializeTunnel(DBRProgressMonitor monitor, DBPDriver driver, DBPConnectionInfo connectionInfo, Shell windowShell)
        throws DBException, IOException;

    void closeTunnel(DBPConnectionInfo connectionInfo, DBRProgressMonitor monitor)
        throws DBException, IOException;

}
