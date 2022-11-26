package org.jkiss.dbeaver.ext.db2;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

import org.jkiss.dbeaver.Log;

/**
 * Retrieves the SQLCA.
 * 
 * <quote>The SQLCA (SQL communications area) is a collection of variables that
 * are updated at the end of the execution of every SQL statement.</quote>
 * 
 * @see https://www.ibm.com/docs/en/db2/11.5?topic=tables-sqlca-sql-communications-area
 */
public class DB2Sqlca {

    private static final Log LOG = Log.getLog(DB2Sqlca.class);

    private Class<?> clazz;
    private Object delegate;

    public DB2Sqlca(Object delegate) {
        this.delegate = delegate;
        this.clazz = delegate.getClass();
    }

    public char[] getSqlWarn() {
        try {
            Method method = clazz.getMethod("getSqlWarn");
            Object result = method.invoke(delegate);
            return (char[]) result;
        } catch (Throwable t) {
            LOG.error("Unable to invoke getSqlWarn()", t);
            return null;
        }
    }

    /**
     * Retrieves the SQLCA from a {@link Connection}
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    static DB2Sqlca from(Connection connection) throws SQLException {
        return DB2Sqlca.from(connection.getWarnings());
    }

    /**
     * Retrieves the SQLCA from a {@link SQLWarning}
     * 
     * @param warning
     * @return
     */
    static DB2Sqlca from(SQLWarning warning) throws SQLException {
        DB2Sqlca sqlca = null;
        try {
            Class<?> clazz = warning.getClass();
            Method getSqlca = clazz.getMethod("getSqlca");
            Object returnValue = getSqlca.invoke(warning);
            if (returnValue != null) {
                sqlca = new DB2Sqlca(returnValue);
            }
        } catch (Throwable t) {
            if (t instanceof SQLException) {
                throw (SQLException) t;
            } else {
                LOG.error("Unable to reflectively access DB2 SQLCA", t);
            }
        }
        return sqlca;
    }

}
