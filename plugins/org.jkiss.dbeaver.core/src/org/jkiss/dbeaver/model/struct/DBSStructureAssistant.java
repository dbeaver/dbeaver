/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.List;

/**
 * DBSStructureAssistant
 */
public interface DBSStructureAssistant
{

    DBSObjectType[] getSupportedObjectTypes();

    Collection<DBSObject> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        Collection<DBSObjectType> objectTypes,
        String objectNameMask,
        int maxResults) throws DBException;

}