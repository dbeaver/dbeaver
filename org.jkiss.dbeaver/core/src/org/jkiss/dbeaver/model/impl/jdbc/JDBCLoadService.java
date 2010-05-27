/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBCLoadService
 */
public abstract class JDBCLoadService<RESULT> extends AbstractLoadService<RESULT> {

    static Log log = LogFactory.getLog(JDBCLoadService.class);

    private DBSObject curObject;
    private boolean cacheResult;
    private Statement statement;
    private RESULT savedResult;
    private boolean resultCached = false;

    protected JDBCLoadService(String serviceName, DBSObject curObject)
    {
        this(serviceName, curObject, false);
    }

    protected JDBCLoadService(String serviceName, DBSObject curObject, boolean cacheResult)
    {
        super(serviceName);
        this.curObject = curObject;
        this.cacheResult = cacheResult;
    }

    public RESULT getCachedResult()
    {
        return savedResult;
    }

    public synchronized RESULT evaluate()
        throws InvocationTargetException, InterruptedException
    {
        if (resultCached) {
            return savedResult;
        }
        try {
            JDBCSession session = (JDBCSession)curObject.getDataSource().getSession(false);
            // Synchronize on session to prevent multiple queries
            // on the same connection from different threads
            synchronized (session) {
                this.statement = session.getConnection().createStatement();
                try {
                    RESULT result = evaluateQuery(this.statement);
                    if (cacheResult) {
                        savedResult = result;
                        resultCached = true;
                    }
                    return result;
                }
                finally {
                    if (this.statement != null) {
                        try {
                            this.statement.close();
                        }
                        finally {
                            this.statement = null;
                        }
                    }
                }
            }
        }
        catch (DBException e) {
            throw new InvocationTargetException(e);
        }
        catch (SQLException e) {
            throw new InvocationTargetException(e);
        }
        finally {
            setProgressMonitor(null);
        }
    }

    @Override
    public boolean cancel()
    {
        if (super.cancel()) {
            return true;
        }
        if (statement != null) {
            try {
                statement.cancel();
                return true;
            }
            catch (SQLException ex) {
                log.error("Statement cancel error", ex);
                return false;
            }
        }
        return false;
    }

    public abstract RESULT evaluateQuery(Statement statement)
        throws InvocationTargetException, InterruptedException, DBException, SQLException;

}
