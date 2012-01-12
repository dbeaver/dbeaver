package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * Class container
 */
public interface WMIClassContainer
{
    boolean hasClasses();

    Collection<WMIClass> getClasses(DBRProgressMonitor monitor)
        throws DBException;

}
