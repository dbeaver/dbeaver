/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.navigator.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Session table
 */
public class LockTable extends DatabaseObjectListControl<DBAServerLock> {

    private DBAServerLockManager<DBAServerLock,DBAServerLockItem> lockManager;

    public LockTable(Composite parent, int style, IWorkbenchSite site, DBAServerLockManager<DBAServerLock,DBAServerLockItem> lockManager)
    {
        super(parent, style, site, CONTENT_PROVIDER);
        this.lockManager = lockManager;
    }

    public DBAServerLockManager<DBAServerLock,DBAServerLockItem> getLockManager() {
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
    protected LoadingJob<Collection<DBAServerLock>> createLoadService()
    {
        return LoadingJob.createService(
            new LoadLocksService(),
            new ObjectsLoadVisualizer());
    }

    public LoadingJob<Void> createAlterService(DBAServerLock lock, Map<String, Object> options)
    {
        return LoadingJob.createService(
            new KillSessionByLockService(lock, options),
            new ObjectActionVisualizer());
    }

    public void init(DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager)
    {
        this.lockManager = lockManager;
    }

    private static IStructuredContentProvider CONTENT_PROVIDER = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection)inputElement).toArray();
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

    private class LoadLocksService extends DatabaseLoadService<Collection<DBAServerLock>> {

        protected LoadLocksService()
        {
            super("Load locks", lockManager.getDataSource());
        }

        @Override
        public Collection<DBAServerLock> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().getDefaultInstance().openIsolatedContext(monitor, "View Locks", null)) {
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
        private final DBAServerLock lock;
        private final Map<String, Object> options;


        protected KillSessionByLockService(DBAServerLock lock, Map<String, Object> options)
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
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().getDefaultInstance().openIsolatedContext(monitor, "View locks", null)) {
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
