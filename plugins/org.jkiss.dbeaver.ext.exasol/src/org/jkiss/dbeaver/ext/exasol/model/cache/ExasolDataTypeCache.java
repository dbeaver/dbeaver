/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019-2019 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.List;

public final class ExasolDataTypeCache
    extends JDBCObjectCache<ExasolDataSource, ExasolDataType> {

    private LongKeyMap<ExasolDataType> dataTypeMap = new LongKeyMap<>();
	
	private static final String SQL_TYPE_CACHE =
        "/*snapshot execution*/ select * from SYS.EXA_SQL_TYPES";

	@NotNull
    @Override
	protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull ExasolDataSource owner) throws SQLException {
		JDBCStatement dbstat = session.createStatement();
		
		dbstat.setQueryString(SQL_TYPE_CACHE);
		
		return dbstat;
	}

	@Override
	protected ExasolDataType fetchObject(@NotNull JDBCSession session, @NotNull ExasolDataSource owner, @NotNull JDBCResultSet resultSet)
			throws SQLException, DBException {
		return new ExasolDataType(owner, resultSet);
	}
	
	@Override
	public void clearCache() {
		// TODO Auto-generated method stub
		super.clearCache();
		dataTypeMap.clear();
		
	}
	
	@Override
	public void removeObject(@NotNull ExasolDataType object, boolean resetFullCache) {
		super.removeObject(object, resetFullCache);
		dataTypeMap.remove(object.getExasolTypeId());
	}
	
	@Override
	public void setCache(@NotNull List<ExasolDataType> objects) {
		super.setCache(objects);
		for (ExasolDataType dt: objects)
		{
			dataTypeMap.put(dt.getExasolTypeId(), dt);
		}
	}
	
	public ExasolDataType getDataTypeId(long id)
	{
		return dataTypeMap.get(id);
	}
	
	
	
	
	
	



}
