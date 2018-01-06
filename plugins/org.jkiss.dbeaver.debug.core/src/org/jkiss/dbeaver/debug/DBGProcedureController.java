package org.jkiss.dbeaver.debug;

import org.jkiss.dbeaver.model.exec.DBCSession;

public abstract class DBGProcedureController<SID_TYPE, OID_TYPE> extends DBGBaseController<SID_TYPE, OID_TYPE> {

    private SID_TYPE sessionId;
    private DBGSessionManager<SID_TYPE, OID_TYPE> dbgSessionManager;
    private DBGSession<? extends DBGSessionInfo<SID_TYPE>, ? extends DBGObject<OID_TYPE>, SID_TYPE, OID_TYPE> dbgSession;

    public DBGProcedureController() {
        super();
    }

    @Override
    protected void afterSessionOpen(DBCSession session) throws DBGException {
        super.afterSessionOpen(session);
        this.dbgSessionManager = initSessionManager(session);
        if (this.dbgSessionManager == null) {
            throw new DBGException("Failed to initialize Debug Session Manager");
        }
        this.dbgSession = createSession(session, dbgSessionManager);
        if (this.dbgSession == null) {
            throw new DBGException("Failed to initialize Debug Session");
        }
        setSessionId(dbgSession.getSessionId());
    }

    protected abstract DBGSessionManager<SID_TYPE, OID_TYPE> initSessionManager(DBCSession session) throws DBGException;

    protected abstract DBGSession<? extends DBGSessionInfo<SID_TYPE>, ? extends DBGObject<OID_TYPE>, SID_TYPE, OID_TYPE> createSession(
            DBCSession session, DBGSessionManager<SID_TYPE, OID_TYPE> sessionManager) throws DBGException;

    @Override
    protected void beforeSessionClose(DBCSession session) throws DBGException {
        if (this.dbgSessionManager != null) {
            this.dbgSessionManager.terminateSession(getSessionId());
            this.dbgSessionManager.dispose();
        }
        this.dbgSessionManager = null;
        super.beforeSessionClose(session);
    }

    public SID_TYPE getSessionId() {
        return sessionId;
    }

    public void setSessionId(SID_TYPE sessionId) {
        this.sessionId = sessionId;
    }

}
