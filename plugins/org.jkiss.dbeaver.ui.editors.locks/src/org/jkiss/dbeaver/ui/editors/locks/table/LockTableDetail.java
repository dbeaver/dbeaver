/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.locks.table;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lock detail table
 */
public class LockTableDetail extends DatabaseObjectListControl<DBAServerLockItem> {

    private static final int RELOAD_DELAY = 50;

    private DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager;
    private boolean reloadPending;
    private final Map<String, Object> options = new HashMap<>(1);

    @NotNull
    public Map<String, Object> getOptions() {
        return options;
    }

    public LockTableDetail(
        @NotNull Composite parent,
        int style,
        @NotNull IWorkbenchSite site,
        @NotNull DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager
    ) {
        super(parent, style, site, CONTENT_PROVIDER);
        this.lockManager = lockManager;
    }

    @NotNull
    public DBAServerLockManager<DBAServerLock, DBAServerLockItem> getLockManager() {
        return lockManager;
    }

    @NotNull
    @Override
    protected String getListConfigId(@NotNull List<Class<?>> classList) {
        return "LocksDetail/" + lockManager.getDataSource().getContainer().getDriver().getId();
    }

    public void clearDetails() {
        options.clear();
        loadData();
    }

    @Override
    protected void loadData(boolean lazy, boolean forUpdate) {
        if (isLoading()) {
            reloadPending = true;
            return;
        }
        reloadPending = false;
        super.loadData(true, forUpdate);
    }

    @NotNull
    @Override
    protected LoadingJob<Collection<DBAServerLockItem>> createLoadService(boolean forUpdate) {
        Map<String, Object> loadOptions = new HashMap<>(options);
        LoadingJob<Collection<DBAServerLockItem>> job = LoadingJob.createService(
            new LoadLockDetailService(loadOptions),
            new ObjectsLoadVisualizer() {
                @Override
                public void completeLoading(@Nullable Collection<DBAServerLockItem> items) {
                    super.completeLoading(options.equals(loadOptions) ? items : List.of());
                }
            });
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(@NotNull IJobChangeEvent event) {
                UIUtils.asyncExec(LockTableDetail.this::loadPendingData);
            }
        });
        return job;
    }

    private void loadPendingData() {
        if (getControl().isDisposed() || !reloadPending) {
            return;
        }
        if (isLoading()) {
            getControl().getDisplay().timerExec(RELOAD_DELAY, this::loadPendingData);
        } else {
            loadData();
        }
    }

    public void init(@NotNull DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager) {
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

        private final Map<String, Object> loadOptions;

        protected LoadLockDetailService(@NotNull Map<String, Object> loadOptions) {
            super(String.format("Load lock details"), lockManager.getDataSource());
            this.loadOptions = loadOptions;
        }

        @NotNull
        @Override
        public Collection<DBAServerLockItem> evaluate(@NotNull DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            if (loadOptions.isEmpty()) {
                return List.of();
            }
            try {
                try (DBCExecutionContext isolatedContext = lockManager.getDataSource().getDefaultInstance()
                    .openIsolatedContext(monitor, "View Lock item", null)) {
                    try (DBCSession session = isolatedContext.openSession(
                        monitor, DBCExecutionPurpose.UTIL, "Retrieve server lock detail")) {
                        return lockManager.getLockItems(session, loadOptions);
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }


}


