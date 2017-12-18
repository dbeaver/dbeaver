/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugObject;

/**
 * @author Andrey.Hitrin
 *
 */
@SuppressWarnings("nls")
public class DebugObjectPostgres implements DebugObject<Integer> {
	
	private final Integer oid;
	
	private final String proname;
	
	private final String owner;
	
	private final String schema;
	
	private final String lang;

	/**
	 * @param oid
	 * @param proname
	 * @param owner
	 * @param schema
	 * @param lang
	 */
	public DebugObjectPostgres(Integer oid, String proname, String owner,
			String schema, String lang)
	{
		super();
		this.oid = oid;
		this.proname = proname;
		this.owner = owner;
		this.schema = schema;
		this.lang = lang;
	}

	public String getOwner()
	{
		return owner;
	}

	public String getSchema()
	{
		return schema;
	}

	public String getLang()
	{
		return lang;
	}

	@Override
	public Integer getID()
	{
		return oid;
	}

	@Override
	public String getName()
	{
		return proname;
	}

	@Override
	public String toString()
	{		
		return "id:" + String.valueOf(oid)  + ", name: `"+ proname + "("+lang+")" + ", user: " + owner +"(" + schema + ")";
	}

	
	
}
