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
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverCategoryDescriptor;
import org.jkiss.dbeaver.registry.DriverManagerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ExperimentalBundles;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.FrameworkUtil;

import java.util.List;
import java.util.*;

/** Data source type gallery used by the new connection wizard. */
public class DataSourceTypeViewer extends Viewer {
    private static final String PROP_SELECTOR_ORDER_BY = "driver.selector.orderBy"; //$NON-NLS-1$
    private static final String PROP_SELECTOR_SHOW_CDATA = "driver.selector.showCData";
    private static final String CDATA_PROVIDER_ID = "cdata";

    public enum OrderBy {
        name(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_name_description),
        score(
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_label,
            UIConnectionMessages.dialog_driver_select_viewer_order_by_score_description);

        private final String label;
        private final String description;

        OrderBy(@NotNull String label, @NotNull String description) {
            this.label = label;
            this.description = description;
        }

        public @NotNull String getLabel() {
            return label;
        }

        public @NotNull String getDescription() {
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
        List<DBPDataSourceType> types = new ArrayList<>();
        for (DBPDataSourceType type : DataSourceProviderRegistry.getInstance().getDataSourceTypes()) {
            List<? extends DBPDriver> availableDrivers = type.getEnabledDrivers().stream()
                .filter(driver -> availableProviders.contains(driver.getProviderDescriptor()) && isDriverAvailable(driver))
                .toList();
            if (DBPDataSourceType.CUSTOM_ID.equals(type.getId())) {
                availableDrivers.stream().map(CustomDriverType::new).forEach(types::add);
            } else if (!availableDrivers.isEmpty()) {
                types.add(type);
            }
        }
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
        addFolder(folderInfos, "popular", UIConnectionMessages.dialog_driver_category_popular_label,
            UIConnectionMessages.dialog_driver_category_popular_tip, types);
        folders.getLast().maxTypes = 12;
        for (DriverCategoryDescriptor category : DriverManagerRegistry.getInstance().getCategories()) {
            if (!category.isPromoted()) {
                continue;
            }
            List<DBPDataSourceType> categoryTypes = types.stream()
                .filter(type -> type.getEnabledDrivers().stream()
                    .filter(DataSourceTypeViewer::isDriverAvailable)
                    .anyMatch(driver -> driver.getCategories().contains(category.getId())))
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
            if (!filter.isEmpty() && folderComposite.getActiveFolder() != folders.getFirst()) {
                folderComposite.switchFolder("all", false);
            }
            applyFilter();
        });
        filterText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(@NotNull KeyEvent e) {
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

    public static boolean isShowCData() {
        return isCDataEnabled() && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_SELECTOR_SHOW_CDATA);
    }

    public static boolean isCDataEnabled() {
        var context = FrameworkUtil.getBundle(DataSourceTypeViewer.class).getBundleContext();
        if (!Boolean.parseBoolean(context.getProperty(ExperimentalBundles.ENABLE_PROPERTY))) {
            return false;
        }
        var registry = ProductConfigRegistry.getInstance();
        return registry.getFeatures().stream()
            .anyMatch(feature -> CDATA_PROVIDER_ID.equals(feature.getId()) && registry.isFeatureEnabled(feature));
    }

    public void setShowCData(boolean showCData) {
        var preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        preferenceStore.setValue(PROP_SELECTOR_SHOW_CDATA, showCData);
        PrefUtils.savePreferenceStore(preferenceStore);
        refresh();
        applyFilter();
        if (site instanceof ISelectionChangedListener listener) {
            listener.selectionChanged(new SelectionChangedEvent(this, getSelection()));
        }
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
                public boolean select(
                    @NotNull Viewer viewer,
                    @Nullable Object parentElement,
                    @NotNull Object element
                ) {
                    DBPDataSourceType type = (DBPDataSourceType) element;
                    String pattern = normalizeSearchText(filter);
                    return CommonUtils.isEmpty(pattern) || matchesSearch(type.getName(), pattern) ||
                        matchesSearch(type.getDescription(), pattern) ||
                        matchesSearch(type.getDataSourceInformation(), pattern) ||
                        type.getEnabledDrivers().stream()
                            .filter(DataSourceTypeViewer::isDriverVisible)
                            .anyMatch(driver ->
                                matchesSearch(driver.getName(), pattern) ||
                                matchesSearch(driver.getFullName(), pattern) ||
                                matchesSearch(driver.getId(), pattern) ||
                                matchesSearch(driver.getDescription(), pattern) ||
                                matchesSearch(driver.getCategory(), pattern) ||
                                driver.getCategories().stream().anyMatch(category ->
                                    matchesSearch(category, pattern)));
                }
            });
        }
    }

    private static boolean matchesSearch(@Nullable String value, @NotNull String pattern) {
        return normalizeSearchText(CommonUtils.toString(value)).contains(pattern);
    }

    private static @NotNull String normalizeSearchText(@NotNull String text) {
        return text.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "");
    }

    public static boolean isDriverAvailable(@NotNull DBPDriver driver) {
        String activityId = driver.getProviderDescriptor().getPluginId() + "/" + driver.getId();
        return PlatformUI.getWorkbench().getActivitySupport().getActivityManager().getIdentifier(activityId).isEnabled() &&
            (!DBWorkbench.isDistributed() || driver.getDefaultDriverLoader().isDriverInstalled());
    }

    public static boolean isDriverVisible(@NotNull DBPDriver driver) {
        return (!CDATA_PROVIDER_ID.equals(driver.getProviderId()) || isShowCData()) && isDriverAvailable(driver);
    }

    public @NotNull TabbedFolderComposite getFolderComposite() {
        return folderComposite;
    }

    @Override
    public @NotNull Control getControl() {
        return composite;
    }

    @Override
    public @Nullable Object getInput() {
        ITabbedFolder folder = folderComposite.getActiveFolder(false);
        return folder instanceof TypeListFolder typeFolder && typeFolder.viewer != null ? typeFolder.viewer.getInput() : null;
    }

    @Override
    public void setInput(@Nullable Object input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ISelection getSelection() {
        ITabbedFolder folder = folderComposite.getActiveFolder(false);
        return folder instanceof TypeListFolder typeFolder && typeFolder.viewer != null ?
            typeFolder.viewer.getSelection() : StructuredSelection.EMPTY;
    }

    @Override
    public void refresh() {
        folders.forEach(TypeListFolder::refresh);
    }

    @Override
    public void setSelection(@NotNull ISelection selection, boolean reveal) {
        ITabbedFolder folder = folderComposite.getActiveFolder();
        if (folder instanceof TypeListFolder typeFolder) {
            typeFolder.viewer.setSelection(selection, reveal);
        }
    }

    private class TypeListFolder implements ITabbedFolder {
        private final List<DBPDataSourceType> types;
        private int maxTypes = Integer.MAX_VALUE;
        private AdvancedListViewer viewer;

        private TypeListFolder(@NotNull List<DBPDataSourceType> types) {
            this.types = new ArrayList<>(types);
        }

        @Override
        public void createControl(@NotNull Composite parent) {
            viewer = new AdvancedListViewer(parent, SWT.NONE);
            viewer.setContentProvider(new ListContentProvider());
            viewer.setLabelProvider(new TypeLabelProvider(viewer));
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
            if (viewer != null) {
                var visibleTypes = types.stream()
                    .filter(type -> type.getEnabledDrivers().stream().anyMatch(DataSourceTypeViewer::isDriverVisible));
                if (maxTypes != Integer.MAX_VALUE) {
                    visibleTypes = visibleTypes.sorted(createComparator(OrderBy.score)).limit(maxTypes);
                }
                viewer.setInput(visibleTypes.sorted(comparator).toList());
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

    private static class TypeLabelProvider extends LabelProvider implements IToolTipProvider {
        private final AdvancedListViewer viewer;

        private TypeLabelProvider(@NotNull AdvancedListViewer viewer) {
            this.viewer = viewer;
        }

        @Override
        public @NotNull Image getImage(@NotNull Object element) {
            DBPDataSourceType type = (DBPDataSourceType) element;
            type.getEnabledDrivers().stream()
                .filter(DataSourceTypeViewer::isDriverVisible)
                .forEach(driver -> DriverIconLoader.load(driver, viewer));
            return DBeaverIcons.getImage(type.getIconBig());
        }

        @Override
        public @NotNull String getText(@NotNull Object element) {
            return ((DBPDataSourceType) element).getName();
        }

        @Override
        public @Nullable String getToolTipText(@NotNull Object element) {
            DBPDataSourceType type = (DBPDataSourceType) element;
            List<? extends DBPDriver> drivers = type.getEnabledDrivers().stream()
                .filter(DataSourceTypeViewer::isDriverVisible)
                .toList();
            if (drivers.size() == 1 && !CommonUtils.isEmpty(drivers.getFirst().getDescription())) {
                return drivers.getFirst().getDescription();
            }
            return type.getDescription();
        }
    }

    private record CustomDriverType(DBPDriver driver) implements DBPDataSourceType {
        @Override
        public @NotNull String getId() {
            return driver.getProviderId() + ":" + driver.getId();
        }

        @Override
        public @NotNull String getName() {
            return driver.getName();
        }

        @Override
        public @Nullable String getDescription() {
            return driver.getDescription();
        }

        @Override
        public @NotNull DBPImage getIcon() {
            return driver.getPlainIcon();
        }

        @Override
        public @NotNull DBPImage getIconBig() {
            return driver.getIconBig();
        }

        @Override
        public @Nullable DBPImage getLogoImage() {
            return driver.getLogoImage();
        }

        @Override
        public @NotNull List<? extends DBPDriver> getDrivers() {
            return List.of(driver);
        }

        @Override
        public @NotNull List<? extends DBPDriver> getEnabledDrivers() {
            return getDrivers();
        }

        @Override
        public int getPromotedScore() {
            return driver.getPromotedScore();
        }
    }
}
