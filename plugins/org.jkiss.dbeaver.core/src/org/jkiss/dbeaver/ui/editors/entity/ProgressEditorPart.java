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
import org.jkiss.dbeaver.ui.editors.DatabaseLazyEditorInput;

/**
 * ProgressEditorPart
 */
public class ProgressEditorPart extends EditorPart {

    private Composite parentControl;
    private Canvas progressCanvas;

    public ProgressEditorPart() {
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
                paintProgress(e);
            }
        });
    }

    private void paintProgress(PaintEvent e) {

    }

}
