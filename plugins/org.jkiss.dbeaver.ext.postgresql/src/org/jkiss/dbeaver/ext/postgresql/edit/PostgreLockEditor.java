/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)   
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import java.util.HashMap;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.lock.PostgreLock;
import org.jkiss.dbeaver.ext.postgresql.model.lock.PostgreLockManager;
import org.jkiss.dbeaver.ext.ui.locks.edit.AbstractLockEditor;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

public class PostgreLockEditor extends AbstractLockEditor{
	
   public static final String pidHold = "hpid";
   public static final String pidWait = "wpid";

	
	@SuppressWarnings("unchecked")
	@Override
	protected LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent) {
		
		DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> lockManager = (DBAServerLockManager) new PostgreLockManager((PostgreDataSource) executionContext.getDataSource());
		
		return new LockManagerViewer(this, parent, lockManager) {
			   @Override
	            protected void contributeToToolbar(DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> sessionManager, IContributionManager contributionManager)
	            {
	                contributionManager.add(new Separator());
	            }
	            @Override
	            protected void onLockSelect(final DBAServerLock<?> lock)
	            {
	                super.onLockSelect(lock);
	                if (lock != null ) {
	                	final PostgreLock pLock = (PostgreLock) lock;
	                	super.refreshDetail( new HashMap<String, Object>() {{ put(pidHold,new Integer(pLock.getHold_pid()));put(pidWait,new Integer(pLock.getWait_pid())); }});
	                }
	            }	
		};
	}
	
}

