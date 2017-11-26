package org.jkiss.dbeaver.postgresql.debug.core;

public interface IPgSqlDebugController {

    public void resume();

    public void suspend();

    public void terminate();

}
