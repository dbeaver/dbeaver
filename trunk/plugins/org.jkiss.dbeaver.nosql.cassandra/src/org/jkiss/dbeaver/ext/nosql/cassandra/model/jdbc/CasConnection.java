package org.jkiss.dbeaver.ext.nosql.cassandra.model.jdbc;

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCPreparedStatementImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.*;

/**
 * Cassandra connection
 */
public class CasConnection extends JDBCConnectionImpl {

    public CasConnection(JDBCConnector connector, DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        super(connector, monitor, purpose, taskTitle);
    }

    @Override
    protected JDBCStatement createStatementImpl(Statement original)
        throws SQLFeatureNotSupportedException
    {
        return new JDBCStatementImpl<Statement>(this, original, false) {
            @Override
            protected JDBCResultSetImpl createResultSetImpl(ResultSet resultSet)
            {
                return new CasResultSet(this, resultSet);
            }
        };
    }

    @Override
    protected JDBCPreparedStatement createPreparedStatementImpl(PreparedStatement original, String sql)
        throws SQLFeatureNotSupportedException
    {
        return new JDBCPreparedStatementImpl(this, original, sql, false) {
            @Override
            protected JDBCResultSetImpl createResultSetImpl(ResultSet resultSet)
            {
                return new CasResultSet(this, resultSet);
            }
        };
    }

    @Override
    protected JDBCCallableStatement createCallableStatementImpl(CallableStatement original, String sql)
        throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException("Cassandra doesn't support stored code");
    }

}
