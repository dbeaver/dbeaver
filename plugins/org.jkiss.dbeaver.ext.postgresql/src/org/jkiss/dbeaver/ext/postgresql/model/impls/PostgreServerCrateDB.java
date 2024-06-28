/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;

/**
 * PostgreServerCrateDB
 */
public class PostgreServerCrateDB extends PostgreServerExtensionBase {

    public PostgreServerCrateDB(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    @Override
    public String getServerTypeName() {
        return "CrateDB";
    }

    @Override
    public boolean supportsOids() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return false;
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
        return false;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return false;
    }

    @Override
    public boolean supportsRules() {
        return true;
    }

    @Override
    public boolean supportsRowLevelSecurity() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsEncodings() {
        return false;
    }

    @Override
    public boolean supportsCollations() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsRoles() {
        return false;
    }

    @Override
    public boolean supportsSessionActivity() {
        return false;
    }

    @Override
    public boolean supportsLocks() {
        return false;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsAggregates() {
        return false;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return false;
    }

    @Override
    public boolean supportsTemplates() {
        return false;
    }

    @Override
    public boolean supportsExplainPlan() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return false;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return false;
    }

    @Override
    public boolean supportsTableStatistics() {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return false;
    }

    @Override
    public boolean supportSerialTypes() {
        return false;
    }

    @Override
    public boolean supportsShowingOfExtraComments() {
        return false;
    }

    @Override
    public boolean supportsSuperusers() {
        return false;
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return true;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return false;
    }

    @Override
    public boolean supportsColumnsRequiring() {
        return false;
    }
}

