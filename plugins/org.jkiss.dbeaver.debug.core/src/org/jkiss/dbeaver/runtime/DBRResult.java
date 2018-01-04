package org.jkiss.dbeaver.runtime;

/**
 * 
 * Represents an operation result
 *
 */
public interface DBRResult {
    
    int OK = 0;
    int INFO = 1;
    int WARNING = 2;
    int ERROR = 3;
    int CANCEL = 3;
    
    int getSeverity();

    String getMessage();
    
    Throwable getException();
    
    boolean isOK();

}
