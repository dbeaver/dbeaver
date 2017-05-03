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
import java.util.List;
import java.util.Map;

/**
 * Session table
 */
public class LockTable extends DatabaseObjectListControl<DBAServerLock<?>> {

    private DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> lockManager;

    public LockTable(Composite parent, int style, IWorkbenchSite site, DBAServerLockManager<DBAServerLock<?>,DBAServerLockItem> lockManager)
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
        return "Locks/" + lockManager.getDataSource().getContainer().getDriver().getId();
    }

    @Override
    protected int getDataLoadTimeout() {
        return 20000;
    }

    @Override
    protected LoadingJob<Collection<DBAServerLock<?>>> createLoadService()
    {
        return LoadingJob.createService(
            new LoadLocksService(),
            new ObjectsLoadVisualizer());
    }

    public LoadingJob<Void> createAlterService(DBAServerLock<?> lock, Map<String, Object> options)
    {
        return LoadingJob.createService(
            new KillSessionByLockService(lock, options),
            new ObjectActionVisualizer());
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

    private class LoadLocksService extends DatabaseLoadService<Collection<DBAServerLock<?>>> {

        protected LoadLocksService()
        {
            super("Load locks", lockManager.getDataSource());
        }

        @Override
        public Collection<DBAServerLock<?>> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().openIsolatedContext(monitor, "View Locks")) {
                    try (DBCSession session = isolatedContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Retrieve server locks")) {
                        return lockManager.getLocks(session, null).values();
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    private class KillSessionByLockService extends DatabaseLoadService<Void> {
        private final DBAServerLock<?> lock;
        private final Map<String, Object> options;


        protected KillSessionByLockService(DBAServerLock<?> lock, Map<String, Object> options)
        {
            super("Kill session by lock", lockManager.getDataSource());
            this.lock = lock;
            this.options = options;
        }

        @Override
        public Void evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().openIsolatedContext(monitor, "View locks")) {
                    try (DBCSession session = isolatedContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Kill server session by lock")) {
                    		lockManager.alterSession(session, this.lock, options);	                       
                        return null;
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }

    }

}
