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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

/**
 * TabbedFolderPageProperties
 */
public class TabbedFolderPageProperties extends TabbedFolderPage implements IRefreshablePart, DBPEventListener {

    protected IWorkbenchPart part;
    protected IDatabaseEditorInput input;
    private Font boldFont;
    private UIJob refreshJob = null;
    private PropertyTreeViewer propertyTree;
    private DBPPropertySource curPropertySource;
    private PropertiesPageControl progressControl;
    private boolean attached;
    private boolean activated;

    public TabbedFolderPageProperties(IWorkbenchPart part, IDatabaseEditorInput input) {
        this.part = part;
        this.input = input;
        this.attached = !DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.ENTITY_EDITOR_DETACH_INFO);
    }

    @Override
    public void createControl(Composite parent)
    {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

        ProgressPageControl ownerProgressControl = null;
        if (this.part instanceof IProgressControlProvider) {
            ownerProgressControl = ((IProgressControlProvider) this.part).getProgressControl();
        }

        progressControl = new PropertiesPageControl(parent);
        if (parent.getLayout() instanceof GridLayout) {
            progressControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        if (ownerProgressControl != null) {
            progressControl.substituteProgressPanel(ownerProgressControl);
        } else {
            progressControl.createProgressPanel();
        }
        propertyTree.getTree().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                progressControl.activate(true);
            }
            @Override
            public void focusLost(FocusEvent e) {
                progressControl.activate(false);
            }
        });
	}

    @Override
    public void setFocus() {
        propertyTree.getControl().setFocus();
    }

    @Override
    public void dispose() {
        if (curPropertySource.getEditableValue() instanceof DBSObject) {
            DBUtils.getObjectRegistry((DBSObject) curPropertySource.getEditableValue()).removeDataSourceListener(this);
        }
        UIUtils.dispose(boldFont);
		super.dispose();
	}

    private void refreshProperties()
    {
        synchronized (this) {
            if (refreshJob == null) {
                refreshJob = new RefreshJob();
                refreshJob.schedule(100);
            }
        }
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        if (input.getDatabaseObject() == event.getObject() && !Boolean.FALSE.equals(event.getEnabled()) && !propertyTree.getControl().isDisposed()) {
            refreshProperties();
        }
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (force) {
            curPropertySource = input.getPropertySource();
            if (propertyTree != null) {
                propertyTree.loadProperties(curPropertySource);
                refreshProperties();
            }
        }
    }

    @Override
    public void aboutToBeShown() {
        if (!activated) {
            activated = true;
            propertyTree.repackColumns();
        }
    }

    private class PropertiesPageControl extends ProgressPageControl implements ILazyPropertyLoadListener {

        PropertiesPageControl(Composite parent) {
            super(parent, SWT.SHEET);

            propertyTree = new PropertyTreeViewer(this, SWT.NONE) {
                @Override
                protected void contributeContextMenu(IMenuManager manager, Object node, String category, DBPPropertyDescriptor property) {
                    fillCustomActions(manager);
                }
            };
            propertyTree.setExtraLabelProvider(new PropertyLabelProvider());
            propertyTree.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
            propertyTree.addSelectionChangedListener(event -> {
                IWorkbenchPartSite site = part.getSite();
                IActionBars actionBars = null;
                if (site instanceof IEditorSite) {
                    actionBars = ((IEditorSite) site).getActionBars();
                } else if (site instanceof IViewSite) {
                    actionBars = ((IViewSite) site).getActionBars();
                }
                if (actionBars != null) {
                    String statusText = null;
                    ISelection propsSelection = propertyTree.getSelection();
                    Object selection = (propsSelection instanceof IStructuredSelection ? ((IStructuredSelection) propsSelection).getFirstElement() : null);
                    DBPPropertyDescriptor prop = propertyTree.getPropertyFromElement(selection);
                    if (prop != null) {
                        statusText = prop.getDescription();
                        if (CommonUtils.isEmpty(statusText)) {
                            statusText = prop.getDisplayName();
                        }
                    }
                    if (CommonUtils.isEmpty(statusText)) {
                        statusText = CommonUtils.toString(selection);
                    }
                    actionBars.getStatusLineManager().setMessage(CommonUtils.notEmpty(statusText));
                }
            });
            PropertiesContributor.getInstance().addLazyListener(this);

            curPropertySource = input.getPropertySource();
            propertyTree.loadProperties(curPropertySource);

            if (input.getDatabaseObject() != null) {
                DBUtils.getObjectRegistry((DBSObject) curPropertySource.getEditableValue()).addDataSourceListener(TabbedFolderPageProperties.this);
            }
            propertyTree.getControl().addDisposeListener(e -> {
                dispose();
                PropertiesContributor.getInstance().removeLazyListener(PropertiesPageControl.this);
            });
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            {
                contributionManager.add(ActionUtils.makeCommandContribution(
                    DBeaverUI.getActiveWorkbenchWindow(),
                    IWorkbenchCommandConstants.FILE_REFRESH));
                contributionManager.add(new Action(isAttached() ? "Detach properties to top panel" : "Move properties to tab", DBeaverIcons.getImageDescriptor(UIIcon.ASTERISK)) {
                    @Override
                    public void run() {
                        detachPropertiesPanel();
                    }
                });
            }
        }

        @Override
        public void handlePropertyLoad(Object object, DBPPropertyDescriptor property, Object propertyValue, boolean completed)
        {
            if (curPropertySource.getEditableValue() == object && !propertyTree.getControl().isDisposed()) {
                refreshProperties();
            }
        }

    }

    private boolean isAttached() {
        return attached;
    }

    private void detachPropertiesPanel() {
        boolean attached = isAttached();
        String title = attached ? "Detach properties to top panel" : "Move properties to tab";
        if (UIUtils.confirmAction(part.getSite().getShell(),
            title,
            title + " will require to reopen editor.\nAre you sure?")) {
            DBPPreferenceStore prefs = DBeaverCore.getGlobalPreferenceStore();
            prefs.setValue(DBeaverPreferences.ENTITY_EDITOR_DETACH_INFO, attached);
            IEditorPart editor;
            if (part.getSite() instanceof MultiPageEditorSite) {
                editor = ((MultiPageEditorSite) part.getSite()).getMultiPageEditor();
            } else {
                editor = (IEditorPart) part;
            }
            if (editor != null) {
                DBNDatabaseNode node = null;
                if (editor.getEditorInput() instanceof DatabaseEditorInput) {
                    node = ((DatabaseEditorInput) editor.getEditorInput()).getNavigatorNode();
                }
                DBeaverUI.getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);

                if (node != null) {
                    NavigatorHandlerObjectOpen.openEntityEditor(node, null, DBeaverUI.getActiveWorkbenchWindow());
                }
            }
        }
    }

    private class PropertyLabelProvider extends ColumnLabelProvider implements IFontProvider {
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof DBPPropertyDescriptor && curPropertySource != null && ((DBPPropertyDescriptor) element).isEditable(curPropertySource.getEditableValue())) {
                return boldFont;
            }
            return null;
        }
    }

    private class RefreshJob extends UIJob {
        RefreshJob()
        {
            super("Refresh properties");
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor)
        {
            if (!propertyTree.getControl().isDisposed()) {
                propertyTree.refresh();
                // Force control redraw (to repaint hyperlinks and other stuff)
                propertyTree.getControl().redraw();
            }

            synchronized (TabbedFolderPageProperties.this) {
                refreshJob = null;
            }
            return Status.OK_STATUS;
        }
    }

}