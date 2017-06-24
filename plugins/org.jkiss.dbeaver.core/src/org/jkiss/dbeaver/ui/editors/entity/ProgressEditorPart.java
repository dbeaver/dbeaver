/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.editors.DatabaseLazyEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

import java.lang.reflect.InvocationTargetException;

/**
 * ProgressEditorPart
 */
public class ProgressEditorPart extends EditorPart {

    private final IDatabaseEditor entityEditor;
    private Composite parentControl;
    private Canvas progressCanvas;

    public ProgressEditorPart(IDatabaseEditor entityEditor) {
        this.entityEditor = entityEditor;
    }

    @Override
    public DatabaseLazyEditorInput getEditorInput() {
        return (DatabaseLazyEditorInput) super.getEditorInput();
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public void createPartControl(Composite parent) {
        this.parentControl = parent;
        createProgressPane(parent);
    }

    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void setFocus() {
        parentControl.setFocus();
    }

    public void setPartName(String newName) {
        super.setPartName(newName);
    }

    public void dispose() {
        super.dispose();
        parentControl = null;
    }

    private void createProgressPane(final Composite parent) {
        progressCanvas = new Canvas(parent, SWT.NONE);
        progressCanvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                e.gc.drawText("Connecting to datasource '" + getEditorInput().getDatabaseObject().getName() + "'...", 5, 5, true);
            }
        });

        InitNodeService loadingService = new InitNodeService();
        LoadingJob<IDatabaseEditorInput> loadJob = LoadingJob.createService(
            loadingService,
            new InitNodeVisualizer(loadingService));
        loadJob.schedule();
    }

    private void initEntityEditor(IDatabaseEditorInput result) {
        if (result == null) {
            return;
        }
        try {
            entityEditor.init(entityEditor.getEditorSite(), result);
            entityEditor.recreateEditorControl();
        } catch (Exception e) {
            DBUserInterface.getInstance().showError("Editor init", "Can't initialize editor", e);
        }
    }

    public Composite destroyAndReturnParent() {
        Composite parent = progressCanvas.getParent();
        progressCanvas.dispose();
        return parent;
    }

    private class InitNodeService extends AbstractLoadService<IDatabaseEditorInput> {

        protected InitNodeService()
        {
            super("Initialize entity editor");
        }

        @Override
        public IDatabaseEditorInput evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                return getEditorInput().initializeRealInput(monitor);
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }

        @Override
        public Object getFamily() {
            return null;
        }
    }

    private class InitNodeVisualizer extends ProgressLoaderVisualizer<IDatabaseEditorInput> {
        public InitNodeVisualizer(InitNodeService loadingService) {
            super(loadingService, ProgressEditorPart.this.progressCanvas);
        }

        @Override
        public void completeLoading(IDatabaseEditorInput result) {
            super.completeLoading(result);
            super.visualizeLoading();
            initEntityEditor(result);
            if (result == null) {
                // Close editor
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        entityEditor.getSite().getWorkbenchWindow().getActivePage().closeEditor(entityEditor, false);
                    }
                });
            }
        }
    }

}
