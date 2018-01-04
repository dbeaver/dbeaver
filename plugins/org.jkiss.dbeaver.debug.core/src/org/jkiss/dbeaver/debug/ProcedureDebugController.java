package org.jkiss.dbeaver.debug;

import java.util.Map;

public class ProcedureDebugController extends DatabaseDebugController {

    private String sessionId;

    public ProcedureDebugController(String datasourceId, String databaseName, Map<String, Object> attributes) {
        super(datasourceId, databaseName, attributes);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

}
