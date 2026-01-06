/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * A part that mimics the underlying editor part but lazily initializes it upon first activation.
 * <p>
 * This is done for performance reasons for usage within multi-page editors. For example, the {@link MultiPageEditorPart}
 * initializes part of a page as soon as it was added using an appropriate method.
 * <p>
 * This can cause severe performance issues when opening several heavyweight parts at once (text editors, etc.).
 */
public class ContentPagePart extends EditorPart implements IPropertyListener, IActiveWorkbenchPart, IRefreshablePart, IAdaptable {
    private final IEditorPart editorPart;

    private Composite composite;
    private boolean activated;

    public ContentPagePart(@NotNull IEditorPart editorPart) {
        this.editorPart = editorPart;
        this.editorPart.addPropertyListener(this);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void createPartControl(Composite composite) {
        this.composite = composite;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (activated) {
            editorPart.doSave(monitor);
        }
    }

    @Override
    public void doSaveAs() {
        if (activated) {
            editorPart.doSaveAs();
        }
    }

    @Override
    public boolean isDirty() {
        return activated && editorPart.isDirty();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return activated && editorPart.isSaveAsAllowed();
    }

    @Override
    public void setFocus() {
        if (activated) {
            editorPart.setFocus();
        }
    }

    @Override
    public String getTitle() {
        return editorPart.getTitle();
    }

    @Override
    public String getTitleToolTip() {
        return editorPart.getTitleToolTip();
    }

    @Override
    public Image getTitleImage() {
        return editorPart.getTitleImage();
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (activated && editorPart instanceof IRefreshablePart) {
            return ((IRefreshablePart) editorPart).refreshPart(source, force);
        } else {
            return RefreshResult.IGNORED;
        }
    }

    @Override
    public void activatePart() {
        if (!activated) {
            try {
                createRealPart();
            } catch (PartInitException e) {
                throw new IllegalStateException("Error initializing real editor part", e);
            } finally {
                activated = true;
            }
        }
    }

    @Override
    public void deactivatePart() {
        // do nothing
    }

    @Override
    public void propertyChanged(Object source, int propId) {
        // delegate changes from the delegated part
        firePropertyChange(propId);
    }

    @Override
    public void dispose() {
        editorPart.removePropertyListener(this);
        editorPart.dispose();
        super.dispose();
    }

    private void createRealPart() throws PartInitException {
        UIUtils.disposeChildControls(composite);
        editorPart.init((IEditorSite) getSite(), getEditorInput());
        editorPart.createPartControl(composite);
        composite.layout(true, true);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return this.editorPart.getAdapter(adapter);
    }
}
