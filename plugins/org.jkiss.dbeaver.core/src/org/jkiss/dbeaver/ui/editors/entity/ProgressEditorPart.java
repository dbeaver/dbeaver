/*
 * DBeaver - Universal Database Manager
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
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.editors.DatabaseLazyEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

import java.lang.reflect.InvocationTargetException;

/**
 * ProgressEditorPart
 */
public class ProgressEditorPart extends EditorPart {

    private final EntityEditor entityEditor;
    private Composite parentControl;
    private Canvas progressCanvas;

    public ProgressEditorPart(EntityEditor entityEditor) {
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
            entityEditor.recreatePages();
        } catch (Exception e) {
            UIUtils.showErrorDialog(entityEditor.getSite().getShell(), "Editor init", "Can't initialize editor", e);
        }
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
