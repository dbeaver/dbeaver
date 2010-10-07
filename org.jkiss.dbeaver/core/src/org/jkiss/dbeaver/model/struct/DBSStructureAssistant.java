/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * DBSStructureAssistant
 */
public interface DBSStructureAssistant
{

    List<DBSTablePath> findTableNames(
        DBRProgressMonitor monitor,
        String tableMask,
        int maxResults) throws DBException;

}