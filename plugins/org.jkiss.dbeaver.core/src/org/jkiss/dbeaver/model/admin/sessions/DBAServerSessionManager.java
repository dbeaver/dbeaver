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
public interface DBAServerSessionManager<SESSION_TYPE extends DBAServerSession> {

    DBPDataSource getDataSource();

    Collection<SESSION_TYPE> getSessions(DBCExecutionContext context, Map<String, Object> options)
        throws DBException;

    void alterSession(DBCExecutionContext context, SESSION_TYPE session, Map<String, Object> options)
        throws DBException;

}
