/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.admin.sessions;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.Map;

/**
 * Session manager
 */
public interface DBAServerSessionManager {

    Collection<DBAServerSession> getSessions(DBCExecutionContext monitor, Map<String, Object> options)
        throws DBException;

    void terminateSession(DBAServerSession session, Map<String, Object> options)
        throws DBException;

}
