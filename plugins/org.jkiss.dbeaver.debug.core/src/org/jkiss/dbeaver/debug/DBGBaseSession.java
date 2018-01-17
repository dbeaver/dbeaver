package org.jkiss.dbeaver.debug;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public abstract class DBGBaseSession implements DBGSession {
    
    private static final Log log = Log.getLog(DBGBaseSession.class);
    
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final DBGBaseController controller;

    protected FutureTask<DBGEvent> task;

    private Thread workerThread = null;

    private JDBCExecutionContext connection = null;

    public DBGBaseSession(DBGBaseController controller) {
        this.controller = controller;
    }
    
    /**
     * Return connection used in debug session 
     * 
     * @return java.sql.Connection
     * @throws DBGException
     */
    //FIXME: rework to DBC API
    protected Connection getConnection() throws DBGException {
        try {
            return ((JDBCExecutionContext) connection).getConnection(new VoidProgressMonitor());
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    //FIXME: should be known during construction
    protected void setConnection(JDBCExecutionContext connection) {
        this.connection = connection;
    }

    public DBGBaseController getController() {
        return controller;
    }
    
    /**
     * Return true if debug session up and running on server 
     * 
     * @return boolean
     */
    public boolean isAttached() {
        return connection != null;
    }

    /**
     * Return true if session up and running debug thread
     * 
     * @return boolean
     */
    public boolean isWaiting() {
        return (task == null ? false : !task.isDone()) && (workerThread == null ? false : workerThread.isAlive());
    }

    /**
     * Return true if session waiting target connection (on breakpoint, after step or continue) in debug thread
     * 
     * @return boolean
     */
    public boolean isDone(){

        if (task == null)
            return true;

        if (task.isDone()) {
            try {
                DBGEvent dbgEvent = task.get();
                getController().fireEvent(dbgEvent);
            } catch (InterruptedException e) {
                log.error("DEBUG INTERRUPT ERROR ",e);
                return false;
            } catch (ExecutionException e) {
                log.error("DEBUG WARNING ",e);
                return false;
            }
            return true;

        }

        return false;

    }
    
    /**
     *  Start thread for SQL command
     * 
     * @param commandSQL
     * @param name
     * @throws DBGException
     */
    protected void runAsync(String commandSQL, String name, DBGEvent event) throws DBGException {

        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {

            connection.setAutoCommit(false);

            DBGWorker worker = new DBGWorker(connection, commandSQL, event);

            task = new FutureTask<DBGEvent>(worker);

            workerThread = new Thread(task);

            workerThread.setName(name);

            workerThread.start();

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);

        }
    }

    public void close() {

        lock.writeLock().lock();

        try {

                if (!isDone()) {
                    task.cancel(true);
                }

                connection.close();

        } finally {
            lock.writeLock().unlock();
        }

    }

}
