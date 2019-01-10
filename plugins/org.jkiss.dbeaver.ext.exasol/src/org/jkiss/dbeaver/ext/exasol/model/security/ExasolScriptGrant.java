/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolScript;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ExasolScriptGrant extends ExasolBaseObjectGrant {
	

	public ExasolScriptGrant(ExasolBaseObjectGrant grant) throws DBException
	{
		super(grant);
	}
	
	@Property(viewable = true, order = 10)
	public ExasolScript getProcedure(DBRProgressMonitor monitor) throws DBException
	{
		return super.getSchema().getProcedure(monitor, super.getObjectName());
	}
	
	@Override
	@Property(viewable = true, order = 70)
	public Boolean getExecuteAuth()
	{
		return super.getExecuteAuth();
	}
	
	//
	// don't show these properties for scripts
	//
	
	@Override
	@Property(hidden = true)
	public Boolean getAlterAuth()
	{
		return super.getAlterAuth();
	}
	
	@Override
	@Property(hidden = true)
	public Boolean getDeleteAuth()
	{
		return super.getDeleteAuth();
	}
	
	@Override
	@Property(hidden = true)
	public Boolean getSelectAuth()
	{
		return super.getSelectAuth();
	}
	
	@Override
	@Property(hidden = true)
	public Boolean getInsertAuth()
	{
		return super.getInsertAuth();
	}
	
	@Override
	@Property(hidden = true)
	public Boolean getUpdateAuth()
	{
		return super.getUpdateAuth();
	}
	
	@Override
	@Property(hidden = true)
	public Boolean getReferencesAuth()
	{
		return super.getReferencesAuth();
	}


}

