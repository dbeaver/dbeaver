/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverCategoryDescriptor;
import org.jkiss.dbeaver.registry.DriverManagerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Data source type gallery used by the new connection wizard. */
public class DataSourceTypeViewer extends Viewer {
    private static final String PROP_SELECTOR_ORDER_BY = "driver.selector.orderBy"; //$NON-NLS-1$

    public enum OrderBy {
        name(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_description),
        score(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_description);

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
    private final Composite composite;
    private final TabbedFolderComposite folderComposite;
    private final List<DBPDataSourceContainer> dataSources = DataSourceRegistry.getAllDataSources();
    private final List<TypeListFolder> folders = new ArrayList<>();
    private Comparator<DBPDataSourceType> comparator;
    private String filter = "";

    public DataSourceTypeViewer(
        @NotNull Composite parent,
        @NotNull Object site,
        @NotNull List<DBPDataSourceProviderDescriptor> providers
    ) {
        this.site = site;
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite filterGroup = UIUtils.createComposite(composite, 1);
        filterGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createExtraFilterControlsBefore(filterGroup);
        Composite filterComposite = new Composite(filterGroup, SWT.NONE);
        filterComposite.setLayout(new GridLayout(2, false));
        filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Text filterText = new Text(filterComposite, SWT.BORDER | SWT.SINGLE);
        filterText.setMessage(UIConnectionMessages.dialog_connection_driver_treecontrol_initialText);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ToolBar toolbar = new ToolBar(filterComposite, SWT.HORIZONTAL);
        ToolItem clearItem = new ToolItem(toolbar, SWT.PUSH);
        clearItem.setImage(DBeaverIcons.getImage(UIIcon.ERASE));
        clearItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> filterText.setText("")));
        createExtraFilterControlsAfter(filterGroup);

        Set<DBPDataSourceProviderDescriptor> availableProviders = new HashSet<>(providers);
        List<DBPDataSourceType> types = DataSourceProviderRegistry.getInstance().getDataSourceTypes().stream()
            .filter(type -> type.getEnabledDrivers().stream()
                .anyMatch(driver -> availableProviders.contains(driver.getProviderDescriptor()) && isDriverAvailable(driver)))
            .map(type -> (DBPDataSourceType) type)
            .toList();
        setOrderBy(getDefaultOrderBy());

        folderComposite = new TabbedFolderComposite(composite, SWT.NONE) {
            @Override
            public boolean setFocus() {
                ITabbedFolder activeFolder = getActiveFolder();
                if (activeFolder != null) {
                    activeFolder.setFocus();
                    return true;
                }
                return super.setFocus();
            }
        };
        folderComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        List<TabbedFolderInfo> folderInfos = new ArrayList<>();
        addFolder(folderInfos, "all", UIConnectionMessages.dialog_driver_category_all_label,
            UIConnectionMessages.dialog_driver_category_all_tip, types);
        List<DBPDataSourceType> popular = new ArrayList<>(types);
        popular.sort(createComparator(OrderBy.score));
        addFolder(folderInfos, "popular", UIConnectionMessages.dialog_driver_category_popular_label,
            UIConnectionMessages.dialog_driver_category_popular_tip, popular.stream().limit(12).toList());
        for (DriverCategoryDescriptor category : DriverManagerRegistry.getInstance().getCategories()) {
            if (!category.isPromoted()) {
                continue;
            }
            List<DBPDataSourceType> categoryTypes = types.stream()
                .filter(type -> type.getCategories().contains(category.getId()))
                .toList();
            if (!categoryTypes.isEmpty()) {
                TypeListFolder folder = new TypeListFolder(categoryTypes);
                folders.add(folder);
                folderInfos.add(new TabbedFolderInfo(category.getId(), category.getName(), category.getIcon(),
                    category.getDescription(), false, folder));
            }
        }
        folderComposite.setFolders(getClass().getSimpleName(), folderInfos.toArray(new TabbedFolderInfo[0]));
        folderComposite.switchFolder("all", false);
        folderComposite.addFolderListener(id -> {
            applyFilter();
            ITabbedFolder activeFolder = folderComposite.getActiveFolder(false);
            ISelection selection = activeFolder instanceof TypeListFolder folder && folder.viewer != null ?
                folder.viewer.getSelection() : StructuredSelection.EMPTY;
            if (site instanceof ISelectionChangedListener listener) {
                listener.selectionChanged(new SelectionChangedEvent(this, selection));
            }
        });

        filterText.addModifyListener(e -> {
            filter = filterText.getText();
            applyFilter();
        });
        filterText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.CR) {
                    folderComposite.setFocus();
                }
            }
        });
    }

    protected void createExtraFilterControlsBefore(@NotNull Composite filterGroup) {
    }

    protected void createExtraFilterControlsAfter(@NotNull Composite filterGroup) {
    }

    private void addFolder(
        @NotNull List<TabbedFolderInfo> folderInfos,
        @NotNull String id,
        @NotNull String name,
        @NotNull String description,
        @NotNull List<DBPDataSourceType> types
    ) {
        TypeListFolder folder = new TypeListFolder(types);
        folders.add(folder);
        folderInfos.add(new TabbedFolderInfo(id, name, DBIcon.TREE_DATABASE, description, false, folder));
    }

    public void setOrderBy(@NotNull OrderBy orderBy) {
        comparator = createComparator(orderBy);
        folders.forEach(TypeListFolder::refresh);
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SELECTOR_ORDER_BY, orderBy.name());
    }

    public static @NotNull OrderBy getDefaultOrderBy() {
        return CommonUtils.valueOf(
            OrderBy.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(PROP_SELECTOR_ORDER_BY),
            OrderBy.score
        );
    }

    @NotNull
    private Comparator<DBPDataSourceType> createComparator(@NotNull OrderBy orderBy) {
        Comparator<DBPDataSourceType> byName = Comparator.comparing(DBPDataSourceType::getName, String.CASE_INSENSITIVE_ORDER);
        if (orderBy == OrderBy.name) {
            return byName;
        }
        return Comparator.<DBPDataSourceType>comparingInt(this::getScore).reversed().thenComparing(byName);
    }

    private int getScore(@NotNull DBPDataSourceType type) {
        Set<DBPDriver> drivers = new HashSet<>(type.getEnabledDrivers());
        return type.getPromotedScore() + (int) dataSources.stream()
            .filter(dataSource -> drivers.contains(dataSource.getDriver()))
            .count();
    }

    private void applyFilter() {
        ITabbedFolder folder = folderComposite.getActiveFolder();
        if (folder instanceof TypeListFolder typeFolder && typeFolder.viewer != null) {
            typeFolder.viewer.setFilters(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    DBPDataSourceType type = (DBPDataSourceType) element;
                    String pattern = filter.toLowerCase(Locale.ENGLISH);
                    return CommonUtils.isEmpty(pattern) || type.getName().toLowerCase(Locale.ENGLISH).contains(pattern) ||
                        CommonUtils.toString(type.getDescription()).toLowerCase(Locale.ENGLISH).contains(pattern) ||
                        type.getDataSourceInformation().toLowerCase(Locale.ENGLISH).contains(pattern) ||
                        type.getEnabledDrivers().stream().anyMatch(driver ->
                            driver.getName().toLowerCase(Locale.ENGLISH).contains(pattern) ||
                            driver.getId().toLowerCase(Locale.ENGLISH).contains(pattern) ||
                            CommonUtils.toString(driver.getDescription()).toLowerCase(Locale.ENGLISH).contains(pattern));
                }
            });
        }
    }

    private static boolean isDriverAvailable(@NotNull DBPDriver driver) {
        return !DBWorkbench.isDistributed() || driver.getDefaultDriverLoader().isDriverInstalled();
    }

    public @NotNull TabbedFolderComposite getFolderComposite() {
        return folderComposite;
    }

    @Override
    public Control getControl() {
        return composite;
    }

    @Override
    public Object getInput() {
        ITabbedFolder folder = folderComposite.getActiveFolder(false);
        return folder instanceof TypeListFolder typeFolder && typeFolder.viewer != null ? typeFolder.viewer.getInput() : null;
    }

    @Override
    public void setInput(Object input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISelection getSelection() {
        ITabbedFolder folder = folderComposite.getActiveFolder(false);
        return folder instanceof TypeListFolder typeFolder && typeFolder.viewer != null ?
            typeFolder.viewer.getSelection() : StructuredSelection.EMPTY;
    }

    @Override
    public void refresh() {
        folders.forEach(TypeListFolder::refresh);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        ITabbedFolder folder = folderComposite.getActiveFolder();
        if (folder instanceof TypeListFolder typeFolder) {
            typeFolder.viewer.setSelection(selection, reveal);
        }
    }

    private class TypeListFolder implements ITabbedFolder {
        private final List<DBPDataSourceType> types;
        private AdvancedListViewer viewer;

        private TypeListFolder(@NotNull List<DBPDataSourceType> types) {
            this.types = new ArrayList<>(types);
        }

        @Override
        public void createControl(Composite parent) {
            viewer = new AdvancedListViewer(parent, SWT.NONE);
            viewer.setContentProvider(new ListContentProvider());
            viewer.setLabelProvider(new LabelProvider() {
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DBPDataSourceType) element).getIconBig());
                }

                @Override
                public String getText(Object element) {
                    return ((DBPDataSourceType) element).getName();
                }
            });
            viewer.addSelectionChangedListener(event -> {
                if (site instanceof ISelectionChangedListener listener) {
                    listener.selectionChanged(event);
                }
            });
            viewer.addDoubleClickListener(event -> {
                if (site instanceof IDoubleClickListener listener) {
                    listener.doubleClick(event);
                }
            });
            refresh();
            applyFilter();
        }

        private void refresh() {
            types.sort(comparator);
            if (viewer != null) {
                viewer.setInput(types);
            }
        }

        @Override
        public void aboutToBeShown() {
            refresh();
        }

        @Override
        public void aboutToBeHidden() {
        }

        @Override
        public void setFocus() {
            viewer.getControl().setFocus();
        }

        @Override
        public void dispose() {
        }
    }
}
