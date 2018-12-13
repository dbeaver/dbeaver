/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.ObjectManagerRegistry;
import org.jkiss.dbeaver.ui.ISearchExecutor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ObjectEditorPageControl extends ProgressPageControl {

    private static final Log log = Log.getLog(ObjectEditorPageControl.class);

    private IDatabaseEditor workbenchPart;
    private IPropertyListener propertyListener;
    private volatile LoadingJob curService = null;

    public ObjectEditorPageControl(Composite parent, int style, IDatabaseEditor workbenchPart)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;
    }

    @Override
    public void disposeControl()
    {
        if (propertyListener != null) {
            getMainEditorPart().removePropertyListener(propertyListener);
            propertyListener = null;
        }
        super.disposeControl();
    }

    public IDatabaseEditor getEditorPart()
    {
        return workbenchPart;
    }

    public boolean isObjectEditable()
    {
        DBCExecutionContext context = getEditorPart().getEditorInput().getExecutionContext();
        if (context == null) {
            return false;
        }
        if (context.getDataSource().getInfo().isReadOnlyMetaData()) {
            return false;
        }
        DBSObject databaseObject = getEditorPart().getEditorInput().getDatabaseObject();
        return databaseObject != null && ObjectManagerRegistry.getInstance().getObjectManager(databaseObject.getClass(), DBEObjectManager.class) != null;
    }

    private IEditorPart getMainEditorPart()
    {
        IWorkbenchPartSite site = workbenchPart.getSite();
        if (site instanceof MultiPageEditorSite) {
            return ((MultiPageEditorSite)site).getMultiPageEditor();
        } else {
            return workbenchPart;
        }
    }

    @Override
    protected ISearchExecutor getSearchRunner() {
        ISearchExecutor searchRunner = super.getSearchRunner();
        if (searchRunner != null) {
            return searchRunner;
        }
        return new ISearchExecutor() {
            @Override
            public boolean performSearch(String searchString, int options) {
                return false;
            }

            @Override
            public void cancelSearch() {

            }
        };
    }

    @Override
    public void fillCustomActions(IContributionManager contributionManager) {
        super.fillCustomActions(contributionManager);
    }

    @Override
    protected synchronized boolean cancelProgress() {
        if (curService != null) {
            curService.cancel();
            return true;
        }
        return false;
    }

    public synchronized <OBJECT_TYPE> void runService(LoadingJob<OBJECT_TYPE> service) {
        curService = service;
        service.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                synchronized(ObjectEditorPageControl.this) {
                    curService = null;
                }
            }
        });
        service.schedule();
    }

    public <OBJECT_TYPE> ObjectsLoadVisualizer<OBJECT_TYPE>  createDefaultLoadVisualizer(DBRRunnableParametrized<OBJECT_TYPE> listener) {
        ObjectsLoadVisualizer<OBJECT_TYPE> visualizer = new ObjectsLoadVisualizer<>();
        if (listener != null) {
            visualizer.addLoadListener(listener);
        }
        return visualizer;
    }

    public class ObjectsLoadVisualizer<OBJECT_TYPE> extends ProgressVisualizer<OBJECT_TYPE> {

        private List<DBRRunnableParametrized<OBJECT_TYPE>> listeners = new ArrayList<>();

        public ObjectsLoadVisualizer() {
        }

        public void addLoadListener(DBRRunnableParametrized<OBJECT_TYPE> listener) {
            listeners.add(listener);
        }

        @Override
        public void completeLoading(OBJECT_TYPE result) {
            super.completeLoading(result);
            if (!listeners.isEmpty()) {
                for (DBRRunnableParametrized<OBJECT_TYPE> listener : listeners) {
                    try {
                        listener.run(result);
                    } catch (InvocationTargetException e) {
                        log.error(e.getTargetException());
                    } catch (InterruptedException e) {
                        // ignore
                        break;
                    }
                }
            }
            listeners.clear();
        }
    }


}
