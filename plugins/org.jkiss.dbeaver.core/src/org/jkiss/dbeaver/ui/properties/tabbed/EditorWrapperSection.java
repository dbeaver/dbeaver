/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.IPropertyChangeNotifier;
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
import org.jkiss.dbeaver.ext.IPropertyChangeReflector;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;

import java.util.List;

/**
 * SourceEditSection
 */
public class EditorWrapperSection extends AbstractPropertySection implements ISectionEditorContributor, ISaveablePart, IRefreshablePart, IAdaptable {

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

        //editor.getSite().setSelectionProvider(selectionProvider);

        //selectionProvider.setSelection(new StructuredSelection());

        actionContributor.setActiveEditor(editor);
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
        actionContributor.setActiveEditor(null);
//        if (sqlViewer != null) {
//            //sqlViewer.enableUndoManager(false);
//        }
    }

    @SuppressWarnings("deprecated")
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

    public void refreshPart(Object source, boolean force)
    {
        // Reload sources
        if (editor instanceof IReusableEditor) {
            ((IReusableEditor)editor).setInput(editorDescriptor.getNestedEditorInput(mainEditor.getEditorInput()));
        }
    }

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

    public void addContributions(List<IEditorActionBarContributor> contributions)
    {
        Class<? extends IEditorActionBarContributor> contributorClass = editorDescriptor.getContributorClass();
        if (contributorClass == null) {
            return;
        }
        for (IEditorActionBarContributor contributor : contributions) {
            if (contributor.getClass() == contributorClass) {
                this.actionContributor = contributor;
                return;
            }
        }
        try {
            this.actionContributor = contributorClass.newInstance();
            contributions.add(this.actionContributor);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public void doSave(IProgressMonitor monitor)
    {
        if (editor != null) {
            editor.doSave(monitor);
        }
    }

    public void doSaveAs()
    {
        if (editor != null) {
            editor.doSaveAs();
        }
    }

    public boolean isDirty()
    {
        return editor != null && editor.isDirty();
    }

    public boolean isSaveAsAllowed()
    {
        return editor != null && editor.isSaveAsAllowed();
    }

    public boolean isSaveOnCloseNeeded()
    {
        return editor != null && editor.isSaveOnCloseNeeded();
    }
}