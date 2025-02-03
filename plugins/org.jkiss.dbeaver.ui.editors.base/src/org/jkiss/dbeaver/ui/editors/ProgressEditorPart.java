/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;

import java.lang.reflect.InvocationTargetException;

/**
 * ProgressEditorPart
 */
public class ProgressEditorPart extends EditorPart {

    private final IDatabaseEditor ownerEditor;
    private Composite parentControl;
    private Composite progressCanvas;
    private volatile LoadingJob<IDatabaseEditorInput> pendingJob;

    public ProgressEditorPart(IDatabaseEditor ownerEditor) {
        this.ownerEditor = ownerEditor;
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
        final DatabaseLazyEditorInput input = getEditorInput();

        progressCanvas = new Composite(parent, SWT.NONE);

        if (input.canLoadImmediately()) {
            scheduleEditorLoad();
        } else {
            createInitializerPlaceholder();
        }
    }

    public synchronized boolean scheduleEditorLoad() {
        if (pendingJob != null) {
            return false;
        }
        InitNodeService loadingService = new InitNodeService();
        pendingJob = LoadingJob.createService(
            loadingService,
            new InitNodeVisualizer(loadingService));
        UIExecutionQueue.queueExec(pendingJob::schedule);
        return true;
    }

    private void createInitializerPlaceholder() {
        final Button button = new Button(progressCanvas, SWT.PUSH);
        button.setText(EditorsMessages.progress_editor_uninitialized_text);
        button.setImage(DBeaverIcons.getImage(UIIcon.SQL_CONNECT));
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            for (Control child : progressCanvas.getChildren()) {
                child.dispose();
            }
            scheduleEditorLoad();
        }));

        final Point buttonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final ControlEditor progressOverlay = new ControlEditor(progressCanvas);
        progressOverlay.minimumWidth = buttonSize.x;
        progressOverlay.minimumHeight = buttonSize.y;
        progressOverlay.setEditor(button);
    }

    private void initEntityEditor(IDatabaseEditorInput result) {
        if (result == null) {
            return;
        }
        try {
            ownerEditor.init(ownerEditor.getEditorSite(), result);
            ownerEditor.recreateEditorControl();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Editor init", "Can't initialize editor", e);
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
            } finally {
                pendingJob = null;
            }
        }

        @Override
        public Object getFamily() {
            return null;
        }
    }

    private class InitNodeVisualizer extends ProgressLoaderVisualizer<IDatabaseEditorInput> implements PaintListener {
        public InitNodeVisualizer(InitNodeService loadingService) {
            super(loadingService, progressCanvas);
            progressCanvas.addPaintListener(this);
        }

        @Override
        public void completeLoading(IDatabaseEditorInput result) {
            super.completeLoading(result);
            super.visualizeLoading();
            if (!progressCanvas.isDisposed()) {
                progressCanvas.removePaintListener(this);
            }
            initEntityEditor(result);
            if (result == null) {
                // Close editor
                UIUtils.asyncExec(() ->
                    ownerEditor.getSite().getWorkbenchWindow().getActivePage().closeEditor(ownerEditor, false));
            } else {
                // Activate entity editor (we have changed inner editors and need to force contexts activation).
                UIUtils.asyncExec(() -> EditorUtils.refreshPartContexts(ownerEditor));
            }
            ActionUtils.evaluatePropertyState("org.jkiss.dbeaver.ui.editors.entity.hasSource");
        }

        @Override
        public void paintControl(PaintEvent e) {
            e.gc.drawText(NLS.bind(EditorsMessages.progress_editor_initializing_text, getEditorInput().getName()), 5, 5, true);
        }
    }

}
