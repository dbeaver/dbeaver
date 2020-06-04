/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * PostgreServerYugabyteDB
 */
public class PostgreServerYugabyteDB extends PostgreServerExtensionBase {

    public PostgreServerYugabyteDB(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "YugabyteDB";
    }

    @Override
    public boolean supportsOids() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return false;
    }

    @Override
    public boolean supportsPartitions() {
        return false;
    }

    @Override
    public boolean supportsInheritance() {
        return false;
    }

    @Override
    public boolean supportsTriggers() {
        return true;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return true;
    }

    @Override
    public boolean supportsRules() {
        return true;
    }

    @Override
    public boolean supportsExtensions() {
        return true;
    }

    @Override
    public boolean supportsEncodings() {
        return true;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Override
    public boolean supportsLocks() {
        return true;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsAggregates() {
        return true;
    }

    @Override
    public boolean supportsResultSetLimits() {
        return true;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return true;
    }

    @Override
    public boolean supportsExplainPlan() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return ture;
    }

    @Override
    public boolean supportsTeblespaceLocation() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return true;
    }
}

