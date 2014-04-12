package org.jkiss.dbeaver.ext.nosql.cassandra.model.jdbc;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Result set
 */
public class CasResultSet extends JDBCResultSetImpl {

    public CasResultSet(JDBCStatementImpl statement, ResultSet original)
    {
        super(statement, original);
    }

    @Override
    protected JDBCResultSetMetaData createMetaDataImpl() throws DBCException
    {
        return new JDBCResultSetMetaData(this) {
            @Override
            protected JDBCColumnMetaData createColumnMetaDataImpl(int index) throws SQLException
            {
                final ResultSetMetaData originalMetaData = getOriginal();

                return new JDBCColumnMetaData(this, index) {

                    //@Property(name = "TTL", category = PROP_CATEGORY_CASSANDRA, order = 20)
                    public Integer getTtl() throws SQLException
                    {
                        try {
                            return (Integer)originalMetaData.getClass().getMethod("getTtl", Integer.TYPE).invoke(originalMetaData, getOrdinalPosition() + 1);
                        } catch (Throwable e) {
                            // Seems to be not supported
                            return null;
                        }
                    }

                    //@Property(name = "Timestamp", category = PROP_CATEGORY_CASSANDRA, order = 21)
                    public Long getTimestamp() throws SQLException
                    {
                        try {
                            return (Long)originalMetaData.getClass().getMethod("getTimestamp", Integer.TYPE).invoke(originalMetaData, getOrdinalPosition() + 1);
                        } catch (Throwable e) {
                            // Seems to be not supported
                            return null;
                        }
                    }
                };
            }
        };
    }
}
