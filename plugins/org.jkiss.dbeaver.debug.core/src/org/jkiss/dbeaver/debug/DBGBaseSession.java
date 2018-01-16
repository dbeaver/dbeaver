package org.jkiss.dbeaver.debug;

public abstract class DBGBaseSession implements DBGSession {
    
    private final DBGBaseController controller;

    public DBGBaseSession(DBGBaseController controller) {
        this.controller = controller;
    }
    
    public DBGBaseController getController() {
        return controller;
    }
    
}
