/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class YashanDBUserProfile extends YashanDBGlobalObject {

	public YashanDBUserProfile(YashanDBDataSource dataSource, ResultSet resultSet) {
		super(dataSource, resultSet != null);
		this.name = JDBCUtils.safeGetString(resultSet, "PROFILE");
	}

	private String name;
	
	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Association
	public Collection<ProfileResource> getResources(DBRProgressMonitor monitor) throws DBException {
		return getDataSource().profileCache.getChildren(monitor, getDataSource(), this);
	}

	public static class ProfileResource extends YashanDBObject<YashanDBUserProfile> {

		private String type;
		private String limit;

		public ProfileResource(YashanDBUserProfile profile, ResultSet resultSet) {
			super(profile, JDBCUtils.safeGetString(resultSet, "RESOURCE_NAME"), true);
			this.type = JDBCUtils.safeGetString(resultSet, "RESOURCE_TYPE");
			this.limit = JDBCUtils.safeGetString(resultSet, "LIMIT");
		}

		@NotNull
		@Override
		@Property(viewable = true, order = 1)
		public String getName() {
			return super.getName();
		}

		@Property(viewable = true, order = 2)
		public String getType() {
			return type;
		}

		@Property(viewable = true, order = 3)
		public String getLimit() {
			return limit;
		}
	}

}
