/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DriverSelectViewer
 *
 * @author Serge Rider
 */
public class DriverSelectViewer extends Viewer {

    private static final int REFRESH_DELAY = 200;

    private static final String PROP_SELECTOR_VIEW_TYPE = "driver.selector.view.mode"; //$NON-NLS-1$
    private static final String PROP_SELECTOR_ORDER_BY = "driver.selector.orderBy"; //$NON-NLS-1$

    private ToolItem switchItem;

    public enum SelectorViewType {
        tree,
        browser
    }

    public enum OrderBy {
        name(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_description
        ),
        score(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_description
        );

        private final String label;
        private final String description;

        OrderBy(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    private final Object site;
    private final List<DBPDataSourceProviderDescriptor> providers;
    private final boolean expandRecent;
    private final SelectorViewType forceViewType;

    private final Composite composite;
    private StructuredViewer selectorViewer;
    private Text filterText;
    private Job refreshJob;
    private Composite selectorComposite;

    private final List<DBPDataSourceContainer> dataSources;
    private OrderBy orderBy;
    private Comparator<DBPDriver> driverComparator;

    private static SelectorViewType getCurrentSelectorViewType() {
        String viewTypeStr = DBWorkbench.getPlatform().getPreferenceStore().getString(PROP_SELECTOR_VIEW_TYPE);
        if (viewTypeStr == null) {
            return SelectorViewType.browser;
        }
        try {
            return SelectorViewType.valueOf(viewTypeStr);
        } catch (IllegalArgumentException e) {
            return SelectorViewType.browser;
        }
    }

    private static void setCurrentSelectorViewType(SelectorViewType viewType) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SELECTOR_VIEW_TYPE, viewType.name());
    }

    public DriverSelectViewer(Composite parent, Object site, List<DBPDataSourceProviderDescriptor> providers, boolean expandRecent) {
        this(parent, site, providers, expandRecent, null);
    }

    public DriverSelectViewer(Composite parent, Object site, List<DBPDataSourceProviderDescriptor> providers, boolean expandRecent, SelectorViewType forceViewType) {
        this.site = site;
        this.providers = providers;
        this.expandRecent = expandRecent;
        this.forceViewType = forceViewType;
        this.dataSources = DataSourceRegistry.getAllDataSources();

        OrderBy defOrderBy = getDefaultOrderBy();
        this.setOrderBy(defOrderBy);

        composite = new Composite(parent, SWT.NONE);
        if (parent.getLayout() instanceof GridLayout) {
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        createFilterControl(composite);

        selectorComposite = UIUtils.createComposite(composite, 1);
        selectorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        selectorComposite.setForeground(filterText.getForeground());
        selectorComposite.setBackground(filterText.getBackground());

        createSelectorControl();

        filterText.addTraverseListener(new TraverseListener() {
            boolean inTraverse;
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (!inTraverse) {
                    inTraverse = true;
                    try {
                        selectorViewer.getControl().traverse(e.detail, e);
                    } finally {
                        inTraverse = false;
                    }
                }
            }
        });

        refreshJob = createRefreshJob();
    }

    public static OrderBy getDefaultOrderBy() {
        return CommonUtils.valueOf(OrderBy.class, DBWorkbench.getPlatform().getPreferenceStore().getString(PROP_SELECTOR_ORDER_BY), OrderBy.score);
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        switch (orderBy) {
            case name:
                this.driverComparator = new DriverUtils.DriverNameComparator();
                break;
            case score:
                this.driverComparator = new DriverUtils.DriverScoreComparator(dataSources);
                break;
        }
        if (selectorViewer instanceof DriverTabbedViewer) {
            ((DriverTabbedViewer) selectorViewer).setListComparator(this.driverComparator);
        }
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SELECTOR_ORDER_BY, orderBy.name());
    }

    private Control getSelectorControl() {
        return selectorViewer.getControl();
    }

    private void createFilterControl(Composite parent) {
        Composite filterGroup = UIUtils.createComposite(parent, 1);
        filterGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createExtraFilterControlsBefore(filterGroup);

        Composite filterComposite = new Composite(filterGroup, SWT.BORDER);
        filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout filterLayout = new GridLayout(2, false);
        filterLayout.marginHeight = 0;
        filterLayout.marginWidth = 0;
        filterComposite.setLayout(filterLayout);
        filterComposite.setFont(composite.getFont());

        filterText = new Text(filterComposite, SWT.SINGLE);
        filterText.setMessage(UIConnectionMessages.dialog_connection_driver_treecontrol_initialText);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterText.addModifyListener(e -> textChanged());
        filterText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.keyCode == SWT.ARROW_DOWN || keyEvent.keyCode == SWT.CR) {
                    getSelectorControl().setFocus();
                }
            }
        });
        filterComposite.setBackground(filterText.getBackground());
        filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createFilterToolbar(filterComposite);

        createExtraFilterControlsAfter(filterGroup);

        UIUtils.asyncExec(() -> filterComposite.layout(true, true));
    }

    protected void createExtraFilterControlsBefore(Composite filterGroup) {

    }

    protected void createExtraFilterControlsAfter(Composite filterGroup) {

    }

    private void createFilterToolbar(Composite parent) {
        // Create browser control toggle
        ToolBar switcherToolbar = new ToolBar(parent, SWT.RIGHT | SWT.HORIZONTAL);
        ToolItem clearItem = new ToolItem(switcherToolbar, SWT.PUSH);
        clearItem.setImage(DBeaverIcons.getImage(UIIcon.ERASE));
        clearItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                clearText();
                filterText.setFocus();
            }
        });

        if (forceViewType == null) {
            switchItem = new ToolItem(switcherToolbar, SWT.CHECK | SWT.DROP_DOWN);
            switchItem.setText("Switch view");
            switchItem.setWidth(UIUtils.getFontHeight(switcherToolbar) * 15);
            switchItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCHEMA));
            switchItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    switchSelectorControl();
                }
            });
        }
        switcherToolbar.setBackground(filterText.getBackground());
    }

    private void clearText() {
        filterText.setText("");
        textChanged();
    }

    @NotNull
    private String getFilterString() {
        return filterText != null ? filterText.getText() : "";
    }

    private void textChanged() {
        // cancel currently running job first, to prevent unnecessary redraw
        refreshJob.cancel();
        refreshJob.schedule(REFRESH_DELAY);
    }

    private void createSelectorControl() {

        selectorComposite.setRedraw(false);
        try {
            if (forceViewType == SelectorViewType.tree || (forceViewType == null && getCurrentSelectorViewType() == SelectorViewType.tree)) {
                if (forceViewType == null) {
                    switchItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCHEMA));
                    switchItem.setText(UIConnectionMessages.viewer_selector_control_text_gallery);
                    switchItem.setSelection(true);
                }

                selectorViewer = new DriverTreeViewer(selectorComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
                selectorViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
                UIUtils.asyncExec(() -> {
                    if (selectorViewer instanceof DriverTreeViewer) {
                        ((DriverTreeViewer) selectorViewer).initDrivers(providers, expandRecent);
                    }
                });
            } else {
                if (forceViewType == null) {
                    switchItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
                    switchItem.setText(UIConnectionMessages.viewer_selector_control_text_classic);
                    switchItem.setSelection(false);
                }

                selectorViewer = new DriverTabbedViewer(selectorComposite, SWT.NONE, dataSources, driverComparator);
                selectorViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

    /*
                selectorViewer.getControl().addTraverseListener(e -> {
                    if (e.detail == SWT.TRAVERSE_ESCAPE) {
                        if (site instanceof IWizardPage) {
                            IWizardContainer container = ((IWizardPage) site).getWizard().getContainer();
                            if (container instanceof Window) {
                                ((Window) container).close();
                            }
                        }
                    }
                });
    */
            }

            selectorViewer.addSelectionChangedListener(event -> {
                if (site instanceof ISelectionChangedListener) {
                    ((ISelectionChangedListener) site).selectionChanged(event);
                }
            });
            selectorViewer.addDoubleClickListener(event -> {
                if (site instanceof IDoubleClickListener) {
                    ((IDoubleClickListener) site).doubleClick(event);
                }
            });
        } finally {
            selectorComposite.setRedraw(true);
        }
    }

    private Object[] collectDrivers(List<DataSourceProviderDescriptor> provs) {
        List<DBPDriver> drivers = new ArrayList<>();
        if (provs != null) {
            for (DataSourceProviderDescriptor provider : provs) {
                drivers.addAll(provider.getEnabledDrivers());
            }
        }
        drivers.sort((o1, o2) -> {
            return o1.getName().compareToIgnoreCase(o2.getName());
        });
        return drivers.toArray(new Object[0]);
    }

    private void switchSelectorControl() {
        selectorComposite.setRedraw(false);
        try {
            SelectorViewType viewType = getCurrentSelectorViewType();
            viewType = viewType == SelectorViewType.tree ? SelectorViewType.browser : SelectorViewType.tree;
            setCurrentSelectorViewType(viewType);

            ISelection curSelection = selectorViewer.getSelection();

            selectorViewer.getControl().dispose();
            createSelectorControl();

            if (curSelection instanceof StructuredSelection && !curSelection.isEmpty()) {
                Object element = ((StructuredSelection) curSelection).getFirstElement();
                UIUtils.asyncExec(() -> {
                    selectorViewer.setSelection(new StructuredSelection(element), true);
                });
            }

            selectorComposite.layout(true, true);
        } finally {
            selectorComposite.setRedraw(true);
        }

        if (!CommonUtils.isEmpty(filterText.getText())) {
            textChanged();
        }
    }

    private WorkbenchJob createRefreshJob() {
        return new WorkbenchJob("Refresh driver filter") {//$NON-NLS-1$
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (getControl().isDisposed()) {
                    return Status.CANCEL_STATUS;
                }

                selectorViewer.getControl().setRedraw(false);
                try {
                    String text = getFilterString();
                    if (CommonUtils.isEmpty(text)) {
                        selectorViewer.setFilters();
                        return Status.OK_STATUS;
                    }

                    DriverFilter driverFilter = new DriverFilter();
                    driverFilter.setPattern(text);

                    selectorViewer.setFilters(driverFilter);
                    if (selectorViewer instanceof AbstractTreeViewer) {
                        ((AbstractTreeViewer) selectorViewer).expandAll();
                    }
                } finally {
                    selectorViewer.getControl().setRedraw(true);
                }

                return Status.OK_STATUS;
            }
        };
    }

    public StructuredViewer getSelectorViewer() {
        return selectorViewer;
    }

    public DriverTabbedViewer getTabbedViewer() {
        return (DriverTabbedViewer) selectorViewer;
    }

    public Control getControl() {
        return composite;
    }

    @Override
    public Object getInput() {
        return selectorViewer.getInput();
    }

    @Override
    public ISelection getSelection() {
        return selectorViewer.getSelection();
    }

    @Override
    public void refresh() {
        selectorViewer.refresh();
    }

    public void refresh(DBPDriver driver) {
        selectorViewer.refresh(driver);
    }

    @Override
    public void setInput(Object input) {
        selectorViewer.setInput(input);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        selectorViewer.setSelection(selection, reveal);
    }

    private static class DriverFilter extends PatternFilter {
        DriverFilter() {
            setIncludeLeadingWildcard(true);
        }

        @Override
        public boolean isElementVisible(Viewer viewer, Object element) {
            if (viewer instanceof AbstractTreeViewer) {
                Object parent = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
                    .getContentProvider()).getParent(element);
                if (parent != null && isLeafMatch(viewer, parent)) {
                    return true;
                }
                return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
            }
            return isLeafMatch(viewer, element);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DriverDescriptor driver) {
                return wordMatches(driver.getName()) ||
                       wordMatches(driver.getDescription()) ||
                       wordMatches(driver.getCategory()) ||
                       wordMatches(driver.getId());
            }
            return super.isLeafMatch(viewer, element);
        }

    }

}
