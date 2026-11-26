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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.TreeViewer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DatabaseObjectSelectionTreeContentProvider extends TreeContentProvider {
    record LoadingObject(@NotNull DBSObject parent) {
    }

    private final TreeViewer viewer;
    private final DBSObjectContainer root;
    private final Runnable onChildrenLoaded;
    @Nullable
    private final DatabaseObjectSelectionTree.ProgressListener progressListener;
    private final Map<DBSObject, List<DBSObject>> children = new ConcurrentHashMap<>();
    private final Map<DBSObject, LoadingJob<Object[]>> jobs = new HashMap<>();

    DatabaseObjectSelectionTreeContentProvider(
        @NotNull TreeViewer viewer,
        @NotNull DBSObjectContainer root,
        @NotNull Runnable onChildrenLoaded,
        @Nullable DatabaseObjectSelectionTree.ProgressListener progressListener
    ) {
        this.viewer = viewer;
        this.root = root;
        this.onChildrenLoaded = onChildrenLoaded;
        this.progressListener = progressListener;
    }

    @NotNull
    @Override
    public Object[] getElements(@NotNull Object inputElement) {
        return getChildren(inputElement);
    }

    @Nullable
    @Override
    public DBSObject getParent(@NotNull Object element) {
        if (element instanceof LoadingObject loading) {
            return loading.parent();
        }
        if (!(element instanceof DBSObject object) || object == root) {
            return null;
        }
        DBSObject parent = object.getParentObject();
        while (parent != null && parent != root && !isVisibleObject(parent)) {
            parent = parent.getParentObject();
        }
        return parent;
    }

    @Override
    public boolean hasChildren(@NotNull Object element) {
        return element instanceof DBSObjectContainer && !(element instanceof DBSEntity) &&
            (!children.containsKey(element) || !children.get(element).isEmpty());
    }

    @NotNull
    List<DBSObject> getCachedChildren(@NotNull DBSObject object) {
        return children.getOrDefault(object, List.of());
    }

    @NotNull
    @Override
    public Object[] getChildren(@NotNull Object parentElement) {
        if (!(parentElement instanceof DBSObjectContainer container) || !hasChildren(container)) {
            return new Object[0];
        }
        List<DBSObject> cached = children.get(container);
        if (cached != null) {
            return cached.toArray();
        }
        if (!jobs.containsKey(container)) {
            startLoading(container);
        }
        return new Object[] {new LoadingObject(container)};
    }

    private void startLoading(@NotNull DBSObjectContainer container) {
        boolean wasIdle = jobs.isEmpty();
        DatabaseLoadService<Object[]> service = new DatabaseLoadService<>(
            UINavigatorMessages.ui_navigator_loading_text_loading.trim() + ": " + container.getName(), container.getDataSource()
        ) {
            @NotNull
            @Override
            public Object[] evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    return loadChildren(monitor, container).toArray();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        LoadingJob<Object[]> job = LoadingJob.createService(service, new ILoadVisualizer<>() {
            @NotNull
            @Override
            public DBRProgressMonitor overwriteMonitor(@NotNull DBRProgressMonitor monitor) {
                return progressListener == null ? monitor : progressListener.wrapMonitor(monitor);
            }

            @Override
            public boolean isCompleted() {
                return viewer.getControl().isDisposed() || !jobs.containsKey(container);
            }

            @Override
            public void visualizeLoading() {
                // The content provider displays the loading placeholder.
            }

            @Override
            public void completeLoading(@Nullable Object[] result) {
                jobs.remove(container);
                if (viewer.getControl().isDisposed()) {
                    return;
                }
                if (progressListener != null && jobs.isEmpty()) {
                    progressListener.loadingFinished();
                }
                if (result == null) {
                    // Avoid retrying a failed/cancelled load on every filter refresh.
                    children.putIfAbsent(container, List.of());
                }
                viewer.refresh();
                onChildrenLoaded.run();
            }
        });
        jobs.put(container, job);
        if (progressListener != null && wasIdle) {
            progressListener.loadingStarted();
        }
        job.schedule();
    }

    @Override
    public void dispose() {
        jobs.values().forEach(LoadingJob::cancel);
        jobs.clear();
        children.clear();
    }

    @NotNull
    synchronized List<DBSObject> loadChildren(@NotNull DBRProgressMonitor monitor, @NotNull DBSObjectContainer container)
        throws DBException, InterruptedException {
        List<DBSObject> cached = children.get(container);
        if (cached != null) {
            return cached;
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
        Set<DBSObject> objects = new LinkedHashSet<>();
        Collection<? extends DBSObject> nested = container.getChildren(monitor);
        if (nested != null) {
            objects.addAll(nested);
        }
        if (container instanceof DBSEntityContainer entityContainer) {
            objects.addAll(entityContainer.getEntities(monitor, null));
        }
        List<DBSObject> result = new ArrayList<>();
        for (DBSObject object : objects) {
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            if (DBUtils.isHiddenObject(object)) {
                continue;
            }
            if (isVisibleObject(object)) {
                result.add(object);
            } else if (!(object instanceof DBSEntity) && object instanceof DBSObjectContainer childContainer) {
                result.addAll(loadChildren(monitor, childContainer));
            }
        }
        result.sort(Comparator.comparing(DBSObject::getName, String.CASE_INSENSITIVE_ORDER));
        List<DBSObject> loaded = List.copyOf(result);
        children.put(container, loaded);
        return loaded;
    }

    static boolean isVisibleObject(@NotNull DBSObject object) {
        return object instanceof DBSCatalog || object instanceof DBSSchema || object instanceof DBSEntity entity &&
            (entity.getEntityType() == DBSEntityType.TABLE || entity.getEntityType() == DBSEntityType.VIEW);
    }

    static boolean isSystemObject(@NotNull DBSObject object) {
        for (DBSObject current = object; current != null; current = current.getParentObject()) {
            if (DBUtils.isSystemObject(current)) {
                return true;
            }
        }
        return false;
    }
}
