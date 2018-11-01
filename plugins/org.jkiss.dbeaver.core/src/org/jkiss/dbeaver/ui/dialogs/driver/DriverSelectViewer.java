/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * DriverSelectViewer
 *
 * @author Serge Rider
 */
public class DriverSelectViewer extends Viewer {

    private static final int REFRESH_DELAY = 200;

    private final Composite composite;
    private DriverTreeViewer driverTree;
    private Text filterText;
    private Control clearButtonControl;
    private String previousFilterText;
    private boolean narrowingDown;
    private Job refreshJob;

    private static final String CLEAR_ICON = "org.jkiss.dbeaver.ui.dialogs.driver.DriverSelectViewer.CLEAR_ICON"; //$NON-NLS-1$
    private static final String DISABLED_CLEAR_ICON = "org.jkiss.dbeaver.ui.dialogs.driver.DriverSelectViewer.DCLEAR_ICON"; //$NON-NLS-1$

    static {
        ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "$nl$/icons/full/etool16/clear_co.png"); //$NON-NLS-1$
        if (descriptor != null) {
            JFaceResources.getImageRegistry().put(CLEAR_ICON, descriptor);
        }
        descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.png"); //$NON-NLS-1$
        if (descriptor != null) {
            JFaceResources.getImageRegistry().put(DISABLED_CLEAR_ICON, descriptor);
        }
    }

    public DriverSelectViewer(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {

        composite = new Composite(parent, SWT.NONE);
        if (parent.getLayout() instanceof GridLayout) {
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        createFilterControl();
        createSelectorControl(site, providers, expandRecent);

        refreshJob = createRefreshJob();
    }

    private Control getSelectorControl() {
        return driverTree.getControl();
    }

    private void createFilterControl() {
        Composite filterComposite = new Composite(composite, SWT.BORDER);
        filterComposite.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        GridLayout filterLayout = new GridLayout(2, false);
        filterLayout.marginHeight = 0;
        filterLayout.marginWidth = 0;
        filterComposite.setLayout(filterLayout);
        filterComposite.setFont(composite.getFont());

        filterText = new Text(filterComposite, SWT.SINGLE);
        filterText.setMessage(CoreMessages.dialog_connection_driver_treecontrol_initialText);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterText.addModifyListener(e -> textChanged());
        filterText.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {
            if (keyEvent.keyCode == SWT.ARROW_DOWN || keyEvent.keyCode == SWT.CR) {
                getSelectorControl().setFocus();
            }
        }));

        createClearTextNew(filterComposite);

        filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    }

    private void createClearTextNew(Composite parent) {
        // only create the button if the text widget doesn't support one
        // natively
        if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) {
            final Image inactiveImage = JFaceResources.getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON).createImage();
            final Image activeImage = JFaceResources.getImageRegistry().getDescriptor(CLEAR_ICON).createImage();
            final Image pressedImage = new Image(composite.getDisplay(), activeImage, SWT.IMAGE_GRAY);

            final Label clearButton = new Label(parent, SWT.NONE);
            clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
            clearButton.setImage(inactiveImage);
            clearButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            clearButton.addMouseListener(new MouseAdapter() {
                private MouseMoveListener fMoveListener;
                @Override
                public void mouseDown(MouseEvent e) {
                    clearButton.setImage(pressedImage);
                    fMoveListener = new MouseMoveListener() {
                        private boolean fMouseInButton = true;
                        @Override
                        public void mouseMove(MouseEvent e) {
                            boolean mouseInButton = isMouseInButton(e);
                            if (mouseInButton != fMouseInButton) {
                                fMouseInButton = mouseInButton;
                                clearButton.setImage(mouseInButton ? pressedImage : inactiveImage);
                            }
                        }
                    };
                    clearButton.addMouseMoveListener(fMoveListener);
                }
                @Override
                public void mouseUp(MouseEvent e) {
                    if (fMoveListener != null) {
                        clearButton.removeMouseMoveListener(fMoveListener);
                        fMoveListener = null;
                        boolean mouseInButton = isMouseInButton(e);
                        clearButton.setImage(mouseInButton ? activeImage : inactiveImage);
                        if (mouseInButton) {
                            clearText();
                            filterText.setFocus();
                        }
                    }
                }
                private boolean isMouseInButton(MouseEvent e) {
                    Point buttonSize = clearButton.getSize();
                    return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
                }
            });
            clearButton.addMouseTrackListener(new MouseTrackAdapter() {
                @Override
                public void mouseEnter(MouseEvent e) {
                    clearButton.setImage(activeImage);
                }
                @Override
                public void mouseExit(MouseEvent e) {
                    clearButton.setImage(inactiveImage);
                }
            });
            clearButton.addDisposeListener(e -> {
                inactiveImage.dispose();
                activeImage.dispose();
                pressedImage.dispose();
            });
            this.clearButtonControl = clearButton;
        }
    }

    private void clearText() {
        setFilterText(""); //$NON-NLS-1$
        textChanged();
    }

    private void setFilterText(String string) {
        if (filterText != null) {
            filterText.setText(string);
            selectAll();
        }
    }

    protected void selectAll() {
        if (filterText != null) {
            filterText.selectAll();
        }
    }

    @NotNull
    private String getFilterString() {
        return filterText != null ? filterText.getText() : "";
    }

    private void textChanged() {
        narrowingDown = previousFilterText == null || previousFilterText.equals(WorkbenchMessages.FilteredTree_FilterMessage) || getFilterString().startsWith(previousFilterText);
        previousFilterText = getFilterString();
        // cancel currently running job first, to prevent unnecessary redraw
        refreshJob.cancel();
        refreshJob.schedule(REFRESH_DELAY);
    }

    private void createSelectorControl(Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        Composite treeComposite = new Composite(composite, SWT.NONE);
        GridLayout treeCompositeLayout = new GridLayout();
        treeCompositeLayout.marginHeight = 0;
        treeCompositeLayout.marginWidth = 0;
        treeComposite.setLayout(treeCompositeLayout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeComposite.setLayoutData(data);

        driverTree = new DriverTreeViewer(treeComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        driverTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.asyncExec(() -> {
            driverTree.initDrivers(site, providers, expandRecent);
        });
    }

    private WorkbenchJob createRefreshJob() {
        return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (getControl().isDisposed()) {
                    return Status.CANCEL_STATUS;
                }

                String text = getFilterString();
                if (CommonUtils.isEmpty(text)) {
                    driverTree.setFilters();
                    return Status.OK_STATUS;
                }

                DriverFilter driverFilter = new DriverFilter();
                driverFilter.setPattern(text);
                driverTree.setFilters(driverFilter);
                driverTree.expandAll();

                return Status.OK_STATUS;
            }
        };
    }

    public Control getControl() {
        return composite;
    }

    @Override
    public Object getInput() {
        return driverTree.getInput();
    }

    @Override
    public ISelection getSelection() {
        return driverTree.getSelection();
    }

    @Override
    public void refresh() {
        driverTree.refresh();
    }

    public void refresh(DBPDriver driver) {
        driverTree.refresh(driver);
    }

    @Override
    public void setInput(Object input) {
        driverTree.setInput(input);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        driverTree.setSelection(selection, reveal);
    }

    private static class DriverFilter extends PatternFilter {
        DriverFilter() {
            setIncludeLeadingWildcard(true);
        }

        @Override
        public boolean isElementVisible(Viewer viewer, Object element) {
            Object parent = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
                .getContentProvider()).getParent(element);
            if (parent != null && isLeafMatch(viewer, parent)) {
                return true;
            }
            return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DriverDescriptor) {
                return wordMatches(((DriverDescriptor) element).getName()) ||
                    wordMatches(((DriverDescriptor) element).getDescription());
            }
            return super.isLeafMatch(viewer, element);
        }

    }

}
