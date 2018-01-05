package org.jkiss.dbeaver.debug;

public class DBGProcedureController extends DBGBaseController {

    private String sessionId;

    public DBGProcedureController() {
        super();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

}
