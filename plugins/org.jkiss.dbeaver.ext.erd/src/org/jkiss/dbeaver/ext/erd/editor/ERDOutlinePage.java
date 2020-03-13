/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * This is a sample implementation of an outline page showing an
 * overview of a graphical editor.
 *
 * @author Gunnar Wagenknecht
 */
public class ERDOutlinePage extends Page implements IContentOutlinePage {

    /**
     * the control of the overview
     */
    private Canvas overview;
    /**
     * the root edit part
     */
    private ScalableFreeformRootEditPart rootEditPart;

    /**
     * the thumbnail
     */
    private Thumbnail thumbnail;

    /**
     * Creates a new ERDOutlinePage instance.
     *
     * @param rootEditPart the root edit part to show the overview from
     */
    public ERDOutlinePage(ScalableFreeformRootEditPart rootEditPart) {
        super();
        this.rootEditPart = rootEditPart;
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {

    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        // create canvas and lws
        overview = new Canvas(parent, SWT.NONE);
        LightweightSystem lws = new LightweightSystem(overview);

        // create thumbnail
        thumbnail =
            new ScrollableThumbnail((Viewport) rootEditPart.getFigure());
        thumbnail.setBorder(new MarginBorder(3));
        thumbnail.setSource(
            rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
        lws.setContents(thumbnail);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#dispose()
     */
    @Override
    public void dispose() {
        if (null != thumbnail)
            thumbnail.deactivate();

        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#getControl()
     */
    @Override
    public Control getControl() {
        return overview;
    }

    @Override
    public ISelection getSelection() {
        return StructuredSelection.EMPTY;
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {

    }

    @Override
    public void setFocus() {
        if (getControl() != null)
            getControl().setFocus();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void setSelection(ISelection selection) {
    }
}
