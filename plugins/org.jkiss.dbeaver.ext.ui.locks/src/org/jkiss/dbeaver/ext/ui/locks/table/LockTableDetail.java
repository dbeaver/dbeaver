/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.ext.ui.locks.table;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lock detail table
 */
public class LockTableDetail extends DatabaseObjectListControl<DBAServerLockItem> {

    private DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> lockManager;
    Map<String, Object> options = new HashMap<String, Object>(1); 
    
    public Map<String, Object> getOptions() {
		return options;
	}

	public LockTableDetail(Composite parent, int style, IWorkbenchSite site, DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> lockManager)
    {
        super(parent, style, site, CONTENT_PROVIDER);
        this.lockManager = lockManager;
    }

    public DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> getLockManager() {
        return lockManager;
    }

    @NotNull
    @Override
    protected String getListConfigId(List<Class<?>> classList) {
        return "LocksDetail/" + lockManager.getDataSource().getContainer().getDriver().getId();
    }

    @Override
    protected LoadingJob<Collection<DBAServerLockItem>> createLoadService()
    {
        return LoadingJob.createService(
            new LoadLockDetailService(),
            new ObjectsLoadVisualizer());
    }

      public void init(DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> lockManager)
    {
        this.lockManager = lockManager;
    }

    private static IStructuredContentProvider CONTENT_PROVIDER = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };
   

	private class LoadLockDetailService extends DatabaseLoadService<Collection<DBAServerLockItem>> {

        protected LoadLockDetailService()
        {
            super(String.format("Load lock details"), lockManager.getDataSource());
        }

        @Override
        public Collection<DBAServerLockItem> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().openIsolatedContext(monitor, "View Lock item")) {
                    try (DBCSession session = isolatedContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Retrieve server lock detail")) {
                        return lockManager.getLockItems(session, options);
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }


}


