/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.manager.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolScript;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public class ExasolScriptGrant extends ExasolBaseObjectGrant {
	

	public ExasolScriptGrant(ExasolBaseObjectGrant grant) throws DBException
	{
		super(grant);
	}
	
	@Property(viewable = true, order = 10)
	public ExasolScript getProcedure() throws DBException
	{
		return super.getSchema().getProcedure(VoidProgressMonitor.INSTANCE, super.getObjectName());
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

