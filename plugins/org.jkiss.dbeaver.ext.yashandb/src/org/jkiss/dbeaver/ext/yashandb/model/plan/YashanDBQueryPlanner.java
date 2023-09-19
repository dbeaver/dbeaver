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
package org.jkiss.dbeaver.ext.yashandb.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

/**
 * YashanDB execution plan node
 */
public class YashanDBQueryPlanner  extends AbstractExecutionPlanSerializer implements DBCQueryPlanner/*, DBCSavedQueryPlanner*/ {
    @Override
    public DBPDataSource getDataSource() {
        return null;
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query, DBCQueryPlannerConfiguration configuration) throws DBException {
        return null;
    }

    @Override
    public DBCPlanStyle getPlanStyle() {
        return null;
    }

    @Override
    public void serialize(Writer planData, DBCPlan plan) throws IOException, InvocationTargetException {

    }

    @Override
    public DBCPlan deserialize(Reader planData) throws IOException, InvocationTargetException {
        return null;
    }

    private final YashanDBDataSource dataSource;

    public YashanDBQueryPlanner(YashanDBDataSource dataSource) {
        this.dataSource = dataSource;
    }


}