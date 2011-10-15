/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;

import java.io.IOException;

/**
 * Version checker job
 */
public class DBeaverVersionChecker extends AbstractJob {

    static final Log log = LogFactory.getLog(DBeaverVersionChecker.class);

    protected DBeaverVersionChecker()
    {
        super("DBeaver new version release checker");
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            VersionDescriptor versionDescriptor = new VersionDescriptor(VersionDescriptor.DEFAULT_VERSION_URL);
            versionDescriptor.getBaseURL();
        } catch (IOException e) {
            log.debug(e);
        }
        return Status.OK_STATUS;
    }
}
