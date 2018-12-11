/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

public class FireBirdPlanAnalyser implements DBCPlan {
	
	private FireBirdDataSource dataSource;
	private JDBCSession session;
	private String query;
	private List<FireBirdPlanNode> rootNodes;

	public FireBirdPlanAnalyser(FireBirdDataSource dataSource, JDBCSession session, String query)
    {
        this.dataSource = dataSource;
        this.session = session;
        this.query = query;
    }
	
	public void explain()
	        throws DBException
	{
		try {
			JDBCPreparedStatement dbStat = session.prepareStatement(getQueryString());
			// Read explained plan
			try {
				String plan = FireBirdUtils.getPlan(dbStat);
				FireBirdPlanBuilder builder = new FireBirdPlanBuilder(plan);
				rootNodes = builder.Build(session);
			} finally {
				dbStat.close();
			}
		} catch (SQLException e) {
			throw new DBCException(e, session.getDataSource());
		}
	}
	
	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	public String getPlanQueryString() throws DBException {
		return null;
	}

	@Override
	public Collection<? extends DBCPlanNode> getPlanNodes() {
		return rootNodes;
	}

}
