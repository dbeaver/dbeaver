/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.properties.tabbed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.services.INestable;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorContributorManager;
import org.jkiss.dbeaver.ext.IDatabaseEditorContributorUser;
import org.jkiss.dbeaver.ext.IPropertyChangeReflector;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;

/**
 * EditorWrapperSection
 */
public class EditorWrapperSection extends AbstractPropertySection implements IDatabaseEditorContributorUser, ISaveablePart, IRefreshablePart, IAdaptable {

    static final Log log = LogFactory.getLog(EditorWrapperSection.class);

    private IDatabaseEditor mainEditor;
    private EntityEditorDescriptor editorDescriptor;
    private IEditorPart editor;
    private IEditorActionBarContributor actionContributor;
    private Composite parent;
    private IEditorSite nestedEditorSite;

    public EditorWrapperSection(IDatabaseEditor mainEditor, EntityEditorDescriptor editorDescriptor)
    {
        this.mainEditor = mainEditor;
        this.editorDescriptor = editorDescriptor;
    }

    public IEditorPart getEditor()
    {
        return editor;
    }

    @Override
    public void createControls(Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage)
    {
		super.createControls(parent, tabbedPropertySheetPage);
        this.parent = parent;
	}

    @Override
    public void dispose()
    {
        if (nestedEditorSite instanceof MultiPageEditorSite) {
            ((MultiPageEditorSite) nestedEditorSite).dispose();
            nestedEditorSite = null;
        }
        super.dispose();
    }

    @Override
    public boolean shouldUseExtraSpace()
    {
		return true;
	}

    private void createEditor()
    {
        editor = editorDescriptor.createEditor();

        final IWorkbenchPartSite ownerSite = this.mainEditor.getSite();
        if (ownerSite instanceof MultiPageEditorSite) {
            final MultiPageEditorPart ownerMultiPageEditor = ((MultiPageEditorSite) ownerSite).getMultiPageEditor();
            nestedEditorSite = new MultiPageEditorSite(ownerMultiPageEditor, editor);

            // Add property change forwarding
            // We need it to tell owner editor about dirty state change
            if (ownerMultiPageEditor instanceof IPropertyChangeReflector) {
                editor.addPropertyListener(new IPropertyListener() {
                    @Override
                    public void propertyChanged(Object source, int propId)
                    {
                        ((IPropertyChangeReflector)ownerMultiPageEditor).handlePropertyChange(propId);
                    }
                });
            }

        } else {
            nestedEditorSite = new SubEditorSite(mainEditor.getEditorSite());
        }

        try {
            editor.init(nestedEditorSite, editorDescriptor.getNestedEditorInput(mainEditor.getEditorInput()));
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(parent.getShell(), "Create SQL viewer", null, e);
        }
        editor.createPartControl(parent);
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (editor != null) {
                    editor.dispose();
                    editor = null;
                }
            }
        });
    }

    @Override
    public void aboutToBeShown()
    {
        if (editor == null) {
            createEditor();
        }
        if (editor instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) editor).activatePart();
        }
        //sqlViewer.enableUndoManager(true);

        editor.getSite().setSelectionProvider(editor.getSite().getSelectionProvider());

        //selectionProvider.setSelection(new StructuredSelection());

        if (actionContributor != null) {
            actionContributor.setActiveEditor(editor);
        }
        activateSectionSite(true);
        //sqlViewer.handleActivate();
    }

    @Override
    public void aboutToBeHidden()
    {
        if (editor instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) editor).deactivatePart();
        }
        activateSectionSite(false);
        if (actionContributor != null) {
            actionContributor.setActiveEditor(null);
        }
//        if (sqlViewer != null) {
//            //sqlViewer.enableUndoManager(false);
//        }
    }

    @SuppressWarnings("deprecation")
    private void activateSectionSite(boolean activate)
    {
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
    public void refreshPart(Object source, boolean force)
    {
        // Reload sources
        if (editor instanceof IReusableEditor) {
            ((IReusableEditor)editor).setInput(editorDescriptor.getNestedEditorInput(mainEditor.getEditorInput()));
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (editor != null) {
            if (adapter.isAssignableFrom(editor.getClass())) {
                return editor;
            } else {
                return editor.getAdapter(adapter);
            }
        }
        return null;
    }

    @Override
    public IEditorActionBarContributor getContributor(IDatabaseEditorContributorManager manager)
    {
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
    public void doSave(IProgressMonitor monitor)
    {
        if (editor != null) {
            editor.doSave(monitor);
        }
    }

    @Override
    public void doSaveAs()
    {
        if (editor != null) {
            editor.doSaveAs();
        }
    }

    @Override
    public boolean isDirty()
    {
        return editor != null && editor.isDirty();
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return editor != null && editor.isSaveAsAllowed();
    }

    @Override
    public boolean isSaveOnCloseNeeded()
    {
        return editor != null && editor.isSaveOnCloseNeeded();
    }
}