package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.DBException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * JDBCLoadService
 */
public abstract class JDBCLoadService<RESULT> extends AbstractLoadService<RESULT> {

    static Log log = LogFactory.getLog(JDBCLoadService.class);

    private static final Object NULL_RESULT = new Object();

    private DBSObject curObject;
    private boolean cacheResult;
    private Statement statement;
    private Object savedResult;

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
        return savedResult == NULL_RESULT ? null : (RESULT)savedResult;
    }

    public synchronized RESULT evaluate()
        throws InvocationTargetException, InterruptedException
    {
        if (savedResult != null) {
            return getCachedResult();
        }
        try {
            JDBCSession session = (JDBCSession)curObject.getDataSource().getSession(false);
            this.statement = session.getConnection().createStatement();
            try {
                RESULT result = evaluateQuery(this.statement);
                if (cacheResult) {
                    savedResult = result == null ? NULL_RESULT : result;
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
