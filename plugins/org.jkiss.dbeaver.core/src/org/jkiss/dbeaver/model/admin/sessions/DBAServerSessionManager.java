/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.admin.sessions;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.Collection;
import java.util.Map;

/**
 * Session manager
 */
public interface DBAServerSessionManager {

    DBPDataSource getDataSource();

    Collection<DBAServerSession> getSessions(DBCExecutionContext monitor, Map<String, Object> options)
        throws DBException;

    void terminateSession(DBAServerSession session, Map<String, Object> options)
        throws DBException;

}
