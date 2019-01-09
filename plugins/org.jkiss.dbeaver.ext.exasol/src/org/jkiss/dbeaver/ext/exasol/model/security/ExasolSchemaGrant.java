/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.meta.Property;

public class ExasolSchemaGrant extends ExasolBaseObjectGrant {

	public ExasolSchemaGrant(ExasolBaseObjectGrant grant)
	{
		super(grant);
	}
	
	
	@Override
	public String getDescription()
	{
		return super.getSchema().getDescription();
	}
	
	@Property(viewable = true, order = 100)
	public Boolean getExecuteAuth()
	{
		return super.getExecuteAuth();
	}


}
