package org.jkiss.dbeaver.debug;

import java.util.EventObject;

public class DBGEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public static final int ATTACH = 0x0001;
    public static final int SUSPEND = 0x0002;
    public static final int RESUME = 0x0004;
    public static final int DETACH = 0x0008;

    public static final int UNSPECIFIED = 0;
    public static final int STEP_INTO = 0x0001;
    public static final int STEP_OVER = 0x0002;
    public static final int STEP_RETURN = 0x0004;
    public static final int STEP_END = 0x0008;

    private int kind;

    private int details;

    public DBGEvent(Object source, int kind) {
        this(source, kind, UNSPECIFIED);
    }

    public DBGEvent(Object source, int kind, int details) {
        super(source);
        this.kind = kind;
        this.details = details;
    }
    
    public int getKind() {
        return kind;
    }
    
    public int getDetails() {
        return details;
    }

}
