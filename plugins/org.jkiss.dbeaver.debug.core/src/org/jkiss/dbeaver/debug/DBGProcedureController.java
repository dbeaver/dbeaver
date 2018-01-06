package org.jkiss.dbeaver.debug;

public abstract class DBGProcedureController<SESSION_ID_TYPE, OBJECT_ID_TYPE> extends DBGBaseController<SESSION_ID_TYPE, OBJECT_ID_TYPE> {

    private SESSION_ID_TYPE sessionId;

    public DBGProcedureController() {
        super();
    }

    public SESSION_ID_TYPE getSessionId() {
        return sessionId;
    }

    public void setSessionId(SESSION_ID_TYPE sessionId) {
        this.sessionId = sessionId;
    }

}
