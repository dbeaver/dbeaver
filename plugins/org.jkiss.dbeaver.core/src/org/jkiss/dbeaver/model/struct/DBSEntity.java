/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntity
 */
public interface DBSEntity extends DBSObject
{
    /**
     * Gets this entity associations
     * @return foreign keys list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets associations which refers this entity
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException;

}
