package org.jkiss.dbeaver.runtime;

public class DefaultResult implements DBRResult {
    
    public DefaultResult OK_RESULT = ok();
    
    private final static String EMPTY_MESSAGE = ""; //$NON-NLS-1$
    
    private final int severity;
    private final String message;
    private final Throwable exception;
    
    public static DefaultResult ok() {
        return new DefaultResult(OK, EMPTY_MESSAGE, null);
    }

    public static DefaultResult info(String message) {
        return new DefaultResult(INFO, message, null);
    }

    public static DefaultResult warning(String message) {
        return new DefaultResult(WARNING, message, null);
    }

    public static DefaultResult error(String message) {
        return new DefaultResult(ERROR, message, null);
    }

    public static DefaultResult error(String message, Throwable exception) {
        return new DefaultResult(ERROR, message, exception);
    }

    public static DefaultResult cancel() {
        return new DefaultResult(CANCEL, EMPTY_MESSAGE, null);
    }

    public DefaultResult(int severity, String message, Throwable exception)
    {
        this.severity = severity;
        this.message = message;
        this.exception = exception;
    }

    @Override
    public int getSeverity()
    {
        return severity;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public Throwable getException()
    {
        return exception;
    }
    
    @Override
    public boolean isOK()
    {
        return severity == OK;
    }
    
}
