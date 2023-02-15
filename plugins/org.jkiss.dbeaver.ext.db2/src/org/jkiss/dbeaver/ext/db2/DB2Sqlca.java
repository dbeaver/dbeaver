/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.BeanUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * {@link DB2Sqlca} is a proxy for {@link com.ibm.db2.jcc.DB2Sqlca} that
 * prevents the need to link directly against the DB2 JDBC Drivers.
 * 
 * <quote>The SQLCA (SQL communications area) is a collection of variables that
 * are updated at the end of the execution of every SQL statement.</quote>
 *
 * @see com.ibm.db2.jcc.DB2Sqlca
 * @see <a href="https://www.ibm.com/docs/en/db2/11.5?topic=tables-sqlca-sql-communications-area">SQLCA (SQL communications area)</a>
 */
public class DB2Sqlca {

    private static final Log LOG = Log.getLog(DB2Sqlca.class);

    private final Object delegate;

    /**
     * Constructor.
     *
     * Constructs a new instance of {@link DB2Sqlca} from an {@link Object}
     * reference to an instance of {@link com.ibm.db2.jcc.DB2Sqlca}.
     *
     * @param delegate
     *            An instance of {@link com.ibm.db2.jcc.DB2Sqlca} acquired
     *            through the DB2 JDBC Driver.
     */
    private DB2Sqlca(Object delegate) {
        this.delegate = delegate;
    }

    /**
     * Retrieves the {@code sqlwarn} vector from the SQLCA.
     *
     * @return the {@code sqlwarn} vector from the SQLCA.
     * @see <a href="https://www.ibm.com/docs/en/db2/11.5?topic=tables-sqlca-sql-communications-area">SQLCA (SQL communications area)</a>
     */
    public @Nullable char[] getSqlWarn() {
        try {
            return (char[]) BeanUtils.invokeObjectMethod(delegate, "getSqlWarn");
        } catch (Throwable t) {
            LOG.error("Unable to invoke getSqlWarn()", t);
            return null;
        }
    }

    /**
     * Retrieves the SQLCA from a {@link Connection}
     *
     * @param connection
     *            {@link Connection} to load an instance of {@link DB2Sqlca}
     *            from.
     * @return An instance of {@link DB2Sqlca} if one can be produced from the
     *         connection, otherwise {@code null}.
     * @see #from(SQLWarning)
     */
    static @Nullable DB2Sqlca from(@NotNull Connection connection) throws SQLException {
        return DB2Sqlca.from(connection.getWarnings());
    }

    /**
     * Retrieves the SQLCA from a {@link SQLWarning}
     *
     * @param warning
     *            {@link Warning} to load an instance of {@link DB2Sqlca} from.
     * @return An instance of {@link DB2Sqlca} if one can be produced from the
     *         connection, otherwise {@code null}.
     */
    static @Nullable DB2Sqlca from(@Nullable SQLWarning warning) throws SQLException {
        if (warning == null) {
            return null;
        }

        try {
            Object value = BeanUtils.invokeObjectMethod(warning, "getSqlca");
            if (value != null) {
                return new DB2Sqlca(value);
            }
        } catch (Throwable t) {
            if (t instanceof SQLException) {
                throw (SQLException) t;
            } else {
                LOG.error("Unable to reflectively access DB2 SQLCA", t);
            }
        }
        return null;
    }

}
