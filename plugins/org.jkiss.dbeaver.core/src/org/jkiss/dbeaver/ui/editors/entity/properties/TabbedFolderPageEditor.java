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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.services.INestable;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderEditorSite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorContributorManager;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorContributorUser;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;

/**
 * EditorWrapperSection
 */
public class TabbedFolderPageEditor extends TabbedFolderPage implements IDatabaseEditorContributorUser, ISaveablePart, IRefreshablePart, IAdaptable {

    private static final Log log = Log.getLog(TabbedFolderPageEditor.class);

    private IDatabaseEditor mainEditor;
    private EntityEditorDescriptor editorDescriptor;
    private IEditorPart editor;
    private IEditorActionBarContributor actionContributor;
    private IEditorSite nestedEditorSite;

    public TabbedFolderPageEditor(IDatabaseEditor mainEditor, EntityEditorDescriptor editorDescriptor) {
        this.mainEditor = mainEditor;
        this.editorDescriptor = editorDescriptor;
    }

    public IEditorPart getEditor() {
        return editor;
    }

    @Override
    public void createControl(Composite parent) {
        editor = editorDescriptor.createEditor();

        final IWorkbenchPartSite ownerSite = this.mainEditor.getSite();
        if (ownerSite instanceof MultiPageEditorSite) {
            final MultiPageEditorPart ownerMultiPageEditor = ((MultiPageEditorSite) ownerSite).getMultiPageEditor();
            nestedEditorSite = new TabbedFolderPageEditorSite(ownerMultiPageEditor, editor);

            // Add property change forwarding
            // We need it to tell owner editor about dirty state change
            if (ownerMultiPageEditor instanceof IPropertyChangeReflector) {
                editor.addPropertyListener(new IPropertyListener() {
                    @Override
                    public void propertyChanged(Object source, int propId) {
                        ((IPropertyChangeReflector) ownerMultiPageEditor).handlePropertyChange(propId);
                    }
                });
            }

        } else {
            nestedEditorSite = new SubEditorSite(mainEditor.getEditorSite());
        }

        try {
            editor.init(nestedEditorSite, editorDescriptor.getNestedEditorInput(mainEditor.getEditorInput()));
        } catch (PartInitException e) {
            DBUserInterface.getInstance().showError("Create SQL viewer", null, e);
        }
        editor.createPartControl(parent);

        if (editor instanceof ISingleControlEditor) {
            // Use focus to active selection provider and contributed actions
            Control editorControl = ((ISingleControlEditor) editor).getEditorControl();
            assert editorControl != null;
            editorControl.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
                    mainEditor.getSite().setSelectionProvider(selectionProvider);
                    //selectionProvider.setSelection(selectionProvider.getSelection());

                    if (actionContributor != null) {
                        actionContributor.setActiveEditor(editor);
                    }
                    activateNestedSite(true);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    activateNestedSite(false);
                    if (actionContributor != null) {
                        actionContributor.setActiveEditor(null);
                    }
                }
            });
        }

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (editor != null) {
                    editor.dispose();
                    editor = null;
                }
            }
        });
    }

    @Override
    public void dispose() {
        if (nestedEditorSite instanceof MultiPageEditorSite) {
            ((MultiPageEditorSite) nestedEditorSite).dispose();
            nestedEditorSite = null;
        }
        super.dispose();
    }

    @Override
    public void setFocus() {
        editor.setFocus();
    }

    @Override
    public void aboutToBeShown() {
        if (editor instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) editor).activatePart();
        }
    }

    @Override
    public void aboutToBeHidden() {
        if (editor instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) editor).deactivatePart();
        }
    }

    @SuppressWarnings("deprecation")
    private void activateNestedSite(boolean activate) {
        if (nestedEditorSite instanceof INestable) {
            if (activate) {
                ((INestable) nestedEditorSite).activate();
            } else {
                ((INestable) nestedEditorSite).deactivate();
            }
        }
        if (nestedEditorSite instanceof MultiPageEditorSite) {
            final IKeyBindingService keyBindingService = ((MultiPageEditorSite) nestedEditorSite).getMultiPageEditor().getEditorSite()
                .getKeyBindingService();
            if (keyBindingService instanceof INestableKeyBindingService) {
                ((INestableKeyBindingService) keyBindingService).activateKeyBindingService(activate ? nestedEditorSite : null);
            }
        }
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (editor instanceof IRefreshablePart) {
            ((IRefreshablePart) editor).refreshPart(source, force);
        }
        // Reload sources
//        if (editor instanceof IReusableEditor) {
//            ((IReusableEditor) editor).setInput(editorDescriptor.getNestedEditorInput(mainEditor.getEditorInput()));
//        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (editor != null) {
            if (adapter.isAssignableFrom(editor.getClass())) {
                return adapter.cast(editor);
            } else {
                return editor.getAdapter(adapter);
            }
        }
        return null;
    }

    @Override
    public IEditorActionBarContributor getContributor(IDatabaseEditorContributorManager manager) {
        Class<? extends IEditorActionBarContributor> contributorClass = editorDescriptor.getContributorClass();
        if (contributorClass == null) {
            return null;
        }
        this.actionContributor = manager.getContributor(contributorClass);
        if (this.actionContributor == null) {
            try {
                this.actionContributor = contributorClass.newInstance();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return this.actionContributor;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (editor != null) {
            editor.doSave(monitor);
        }
    }

    @Override
    public void doSaveAs() {
        if (editor != null) {
            editor.doSaveAs();
        }
    }

    @Override
    public boolean isDirty() {
        return editor != null && editor.isDirty();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return editor != null && editor.isSaveAsAllowed();
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return editor != null && editor.isSaveOnCloseNeeded();
    }

    private class TabbedFolderPageEditorSite extends MultiPageEditorSite implements ITabbedFolderEditorSite {

        public TabbedFolderPageEditorSite(MultiPageEditorPart multiPageEditor, IEditorPart editor) {
            super(multiPageEditor, editor);
        }

        @NotNull
        @Override
        public IEditorPart getFolderEditor() {
            return mainEditor;
        }
    }

}