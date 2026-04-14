/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.timeplus.model.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCObjectSupplier;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;

import java.sql.SQLException;
import java.sql.Statement;

public class TimeplusJdbcFactory extends JDBCFactoryDefault {

    @NotNull
    @Override
    public JDBCStatement createStatement(
        @NotNull JDBCSession session,
        @NotNull JDBCObjectSupplier<Statement> stmtSupplier,
        boolean disableLogging
    ) throws SQLException {
        return new TimeplusJdbcStatement(session, stmtSupplier, disableLogging);
    }
}
