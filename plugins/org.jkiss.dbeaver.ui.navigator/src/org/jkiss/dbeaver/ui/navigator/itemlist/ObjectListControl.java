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
package org.jkiss.dbeaver.ui.navigator.itemlist;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNNodeReference;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.*;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * ObjectListControl
 */
public abstract class ObjectListControl<OBJECT_TYPE> extends ProgressPageControl implements IClipboardSource {
    private static final Log log = Log.getLog(ObjectListControl.class);

    private final static LazyValue DEF_LAZY_VALUE = new LazyValue("..."); //$NON-NLS-1$
    private final static int LAZY_LOAD_DELAY = 100;
    private final static Object NULL_VALUE = new Object();
    private static final String EMPTY_STRING = "";
    private static final String EMPTY_GROUPING_LABEL = "<None>";

    private boolean isFitWidth;
    private boolean isTree;

    private ColumnViewer itemsViewer;
    //private ColumnViewerEditor itemsEditor;
    private IDoubleClickListener doubleClickHandler;
    private PropertySourceAbstract listPropertySource;

    private ObjectViewerRenderer renderer;
    protected ViewerColumnController<ObjectColumn, Object> columnController;

    // Sample flag. True only when initial content is packed. Used to provide actual cell data to Tree/Table pack() methods
    // After content is loaded is always false (and all hyperlink cells have empty text)
    private transient boolean sampleItems = false;

    private volatile OBJECT_TYPE curListObject;
    private volatile LoadingJob<Collection<OBJECT_TYPE>> loadingJob;

    private Job lazyLoadingJob = null;
    private Map<OBJECT_TYPE, List<ObjectColumn>> lazyObjects;
    private final Map<OBJECT_TYPE, Map<String, Object>> lazyCache = new IdentityHashMap<>();
    private volatile boolean lazyLoadCanceled;
    private List<OBJECT_TYPE> objectList = null;
    private Object focusObject;
    private ObjectColumn focusColumn;

    private ObjectColumn groupingColumn;
    private IContentProvider originalContentProvider;

    public ObjectListControl(
        Composite parent,
        int style,
        IContentProvider contentProvider)
    {
        super(parent, style);

        this.isFitWidth = false;
        this.originalContentProvider = contentProvider;

        int viewerStyle = getDefaultListStyle();
        if ((style & SWT.SHEET) == 0) {
            viewerStyle |= SWT.BORDER;
        }

        EditorActivationStrategy editorActivationStrategy;
        final TraverseListener traverseListener = e -> {
            if (e.detail == SWT.TRAVERSE_RETURN && doubleClickHandler != null) {
                doubleClickHandler.doubleClick(new DoubleClickEvent(itemsViewer, itemsViewer.getSelection()));
                e.doit = false;
            }
        };

        boolean showTableGrid = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID);
        if (UIStyles.isDarkTheme()) {
            // Do not show grid in dark theme. It is awful
            showTableGrid = false;
        }
        if (contentProvider instanceof ITreeContentProvider) {
            isTree = true;
        }
        if (contentProvider instanceof ITreeContentProvider) {
            TreeViewer treeViewer = new TreeViewer(this, viewerStyle);
            final Tree tree = treeViewer.getTree();
            tree.setHeaderVisible(true);
            if (showTableGrid) {
                tree.setLinesVisible(true);
            }
            itemsViewer = treeViewer;
            editorActivationStrategy = new EditorActivationStrategy(treeViewer);
            TreeViewerEditor.create(treeViewer, editorActivationStrategy, ColumnViewerEditor.TABBING_CYCLE_IN_ROW);
            // We need measure item listener to prevent collapse/expand on double click
            // Looks like a bug in SWT: http://www.eclipse.org/forums/index.php/t/257325/
            treeViewer.getControl().addListener(SWT.MeasureItem, event -> {
                // Just do nothing
            });
            tree.addTraverseListener(traverseListener);
        } else {
            TableViewer tableViewer;
            if ((viewerStyle & SWT.CHECK) == SWT.CHECK) {
                tableViewer = CheckboxTableViewer.newCheckList(this, viewerStyle);
            } else {
                tableViewer = new TableViewer(this, viewerStyle);
            }
            final Table table = tableViewer.getTable();
            table.setHeaderVisible(true);
            if (showTableGrid) {
                table.setLinesVisible(true);
            }
            itemsViewer = tableViewer;
            //UIUtils.applyCustomTolTips(table);
            //itemsEditor = new TableEditor(table);
            editorActivationStrategy = new EditorActivationStrategy(tableViewer);
            TableViewerEditor.create(tableViewer, editorActivationStrategy, ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.TABBING_HORIZONTAL);
            table.addTraverseListener(traverseListener);
        }

        //editorActivationStrategy.setEnableEditorActivationWithKeyboard(true);
        renderer = createRenderer();
        itemsViewer.getColumnViewerEditor().addEditorActivationListener(new EditorActivationListener());

        itemsViewer.setContentProvider(contentProvider);
        //itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (doubleClickHandler != null) {
                    // Uee provided double click
                    doubleClickHandler.doubleClick(new DoubleClickEvent(itemsViewer, itemsViewer.getSelection()));
                }
            }
        });
        itemsViewer.getControl().addListener(SWT.PaintItem, new PaintListener());
        GridData gd = new GridData(GridData.FILL_BOTH);
        itemsViewer.getControl().setLayoutData(gd);
        //PropertiesContributor.getInstance().addLazyListener(this);
        new DefaultViewerToolTipSupport(itemsViewer);

        // Add selection listener
        itemsViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.isEmpty()) {
                setCurListObject(null);
            } else {
                setCurListObject((OBJECT_TYPE) selection.getFirstElement());
            }

            String status;
            if (selection.isEmpty()) {
                status = EMPTY_STRING; //$NON-NLS-1$
            } else if (selection.size() == 1) {
                Object selectedNode = selection.getFirstElement();
                status = ObjectViewerRenderer.getCellString(selectedNode, false);
            } else {
                status = NLS.bind(UINavigatorMessages.controls_object_list_status_objects, selection.size());
            }
            setInfo(status);
        });

        UIUtils.installAndUpdateMainFont(itemsViewer.getControl());
    }

    /**
     * Used to save/load columns configuration.
     * Must depend on object types
     *
     * @param classList classes of objects in the list
     */
    @NotNull
    protected abstract String getListConfigId(List<Class<?>> classList);

    protected int getDefaultListStyle() {
        return SWT.MULTI | SWT.FULL_SELECTION;
    }

    public ObjectViewerRenderer getRenderer() {
        return renderer;
    }

    public PropertySourceAbstract getListPropertySource() {
        if (this.listPropertySource == null) {
            this.listPropertySource = createListPropertySource();
        }
        return listPropertySource;
    }

    protected PropertySourceAbstract createListPropertySource() {
        return new DefaultListPropertySource();
    }

    protected CellLabelProvider getColumnLabelProvider(ObjectColumn objectColumn) {
        return new ObjectColumnLabelProvider(objectColumn);
    }

    @Override
    protected boolean cancelProgress() {
        synchronized (this) {
            if (loadingJob != null) {
                loadingJob.cancel();
                return true;
            }
        }
        return false;
    }

    public OBJECT_TYPE getCurrentListObject() {
        return curListObject;
    }

    protected void setCurListObject(@Nullable OBJECT_TYPE curListObject) {
        this.curListObject = curListObject;
    }

    public ColumnViewer getItemsViewer() {
        return itemsViewer;
    }


    public Composite getControl() {
        // Both table and tree are composites so its ok
        return (Composite) itemsViewer.getControl();
    }

    public ISelectionProvider getSelectionProvider() {
        return itemsViewer;
    }

    protected ObjectColumn getColumnByIndex(int index) {
        return columnController.getColumnData(index);
    }

    public void setFitWidth(boolean fitWidth) {
        isFitWidth = fitWidth;
    }

    public boolean supportsDataGrouping() {
        return true;
    }

    @Override
    public void disposeControl() {
        synchronized (this) {
            if (loadingJob != null) {
                // Cancel running job
                loadingJob.cancel();
                loadingJob = null;
            }
        }
        renderer.dispose();
        super.disposeControl();
    }

    public synchronized boolean isLoading() {
        return loadingJob != null;
    }

    public void loadData() {
        loadData(true, false);
    }

    public void loadData(boolean lazy) {
        loadData(lazy, false);
    }

    protected void loadData(boolean lazy, boolean forUpdate) {
        if (this.loadingJob != null) {
            int dataLoadUpdatePeriod = 200;
            int dataLoadTimes = getDataLoadTimeout() / dataLoadUpdatePeriod;
            try {
                for (int i = 0; i < dataLoadTimes; i++) {
                    Thread.sleep(dataLoadUpdatePeriod);
                    if (this.loadingJob == null) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // interrupted
            }
            if (loadingJob != null) {
                DBWorkbench.getPlatformUI().showMessageBox("Load", "Service is busy", true);
                return;
            }
            return;
        }
        getListPropertySource();

        clearLazyCache();
        this.lazyLoadCanceled = false;

        if (lazy) {
            // start loading service
            synchronized (this) {
                this.loadingJob = createLoadService(forUpdate);
                if (this.loadingJob != null) {
                    this.loadingJob.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event) {
                            loadingJob = null;
                        }
                    });
                    this.loadingJob.schedule(LAZY_LOAD_DELAY);
                }
            }
        } else {
            // Load data synchronously
            final LoadingJob<Collection<OBJECT_TYPE>> loadService = createLoadService(forUpdate);
            if (loadService != null) {
                loadService.syncRun();
            }
        }
    }

    public ViewerColumnController<ObjectColumn, Object> getColumnController() {
        return columnController;
    }

    protected int getDataLoadTimeout() {
        return 10000;
    }

    protected void setListData(Collection<OBJECT_TYPE> items, boolean append, boolean forUpdate) {
        setListData(items, append, forUpdate, false);
    }

    protected void setListData(Collection<OBJECT_TYPE> items, boolean append, boolean forUpdate, boolean forceUpdateItems) {
        if (items == null) {
            return;
        }
        final Control itemsControl = itemsViewer.getControl();
        if (itemsControl.isDisposed()) {
            return;
        }

        itemsControl.setRedraw(false);
        try {
            final boolean reload = !append && (CommonUtils.isEmpty(objectList)) || (columnController == null);

            {
                // Collect list of items' classes
                final List<Class<?>> classList = new ArrayList<>();
                Class<?>[] baseTypes = getListBaseTypes(items);
                if (!ArrayUtils.isEmpty(baseTypes)) {
                    Collections.addAll(classList, baseTypes);
                }
                if (!CommonUtils.isEmpty(items)) {
                    for (OBJECT_TYPE item : items) {
                        Object object = getObjectValue(item);
                        if (object != null) {
                            Class<?> theClass = ObjectPropertyDescriptor.getObjectClass(object);
                            if (!classList.contains(theClass)) {
                                // Remove all base classes if we have sub class
                                // But keep interfaces because we may have multiple implementations of e.g. DBPNamedObject
                                // and we need to show "Name" instead of particular name props
                                for (int i = 0; i < classList.size(); i++) {
                                    Class<?> c = classList.get(i);
                                    if (!c.isInterface() && c.isAssignableFrom(theClass)) {
                                        classList.remove(i);
                                    } else {
                                        i++;
                                    }
                                }
                                classList.add(theClass);
                            }
                        }
                        if (isTree) {
                            Map<OBJECT_TYPE, Boolean> collectedSet = new IdentityHashMap<>();
                            collectItemClasses(item, classList, collectedSet);
                        }
                    }
                }

                IPropertyFilter propertyFilter = new DataSourcePropertyFilter(
                    ObjectListControl.this instanceof DBPDataSourceContainerProvider ?
                        ((DBPDataSourceContainerProvider) ObjectListControl.this).getDataSourceContainer() :
                        null);

                // Collect all properties
                PropertySourceAbstract propertySource = getListPropertySource();
                List<ObjectPropertyDescriptor> allProps = ObjectAttributeDescriptor.extractAnnotations(propertySource, classList, propertyFilter);
                if (!CommonUtils.isEmpty(items)) {
                    // Remove hidden properties (we need to check them against all items)
                    try {
                        allProps.removeIf(p -> {
                            for (OBJECT_TYPE item : items) {
                                Object objectValue = getObjectValue(item);
                                if (objectValue != null && p.isPropertyVisible(objectValue, objectValue)) {
                                    return false;
                                }
                            }
                            return true;
                        });
                    } catch (Throwable e) {
                        log.debug(e);
                    }
                }

                if (reload) {
                    clearListData();
                    columnController = new GroupingViewerColumnController<>(getListConfigId(classList), getItemsViewer());
                }

                // Create columns from classes' annotations
                for (ObjectPropertyDescriptor prop : allProps) {
                    if (!propertySource.hasProperty(prop)) {
                        if (prop.isOptional()) {
                            // Check whether at least one item has this property
                            boolean propHasValue = false;
                            if (!CommonUtils.isEmpty(items)) {
                                for (OBJECT_TYPE item : items) {
                                    try {
                                        Object propValue = prop.readValue(getObjectValue(item), null, true);
                                        if (propValue != null) {
                                            propHasValue = true;
                                            break;
                                        }
                                    } catch (Throwable e) {
                                        // Just ignore this
                                    }
                                }
                            }
                            if (!propHasValue) {
                                continue;
                            }
                        }
                        propertySource.addProperty(prop);
                        createColumn(prop);
                    }
                }
            }

            addExtraColumns(columnController, items);

            if (itemsControl.isDisposed()) {
                return;
            }
            if (reload) {
                columnController.createColumns(false);
            }
            if (reload || objectList.isEmpty()) {
                // Set viewer content
                objectList = createViewerInput(items);

                // Pack columns
                sampleItems = true;
                try {
                    List<OBJECT_TYPE> sampleList;
                    if (objectList.size() > 200) {
                        sampleList = objectList.subList(0, 100);
                    } else {
                        sampleList = objectList;
                    }
                    itemsViewer.setInput(createViewerInput(sampleList));

                    if (isTree) {
                        ((TreeViewer) itemsViewer).expandToLevel(4);
                    }
                    if (reload) {
                        columnController.repackColumns();
                    }
                } finally {
                    sampleItems = false;
                }
                // Set real content
                itemsViewer.setInput(objectList);
            } else if (items != null) {
                if (append) {
                    // Simply append new list to the tail
                    for (OBJECT_TYPE newObject : items) {
                        if (!objectList.contains(newObject)) {
                            objectList.add(newObject);
                        }
                    }
                } else {
                    // Update object list
                    if (!objectList.equals(items) || forceUpdateItems) {
                        int newListSize = items.size();
                        int itemIndex = 0;
                        for (OBJECT_TYPE newObject : items) {
                            if (itemIndex >= objectList.size()) {
                                // Add to tail
                                objectList.add(itemIndex, newObject);
                            } else {
                                OBJECT_TYPE oldObject = objectList.get(itemIndex);
                                if (!CommonUtils.equalObjects(oldObject, newObject) || forceUpdateItems) {
                                    // Replace old object
                                    objectList.set(itemIndex, newObject);
                                }
                            }
                            itemIndex++;
                        }
                        while (objectList.size() > newListSize) {
                            objectList.remove(objectList.size() - 1);
                        }
                    }
                }

                itemsViewer.refresh();
            }
        } finally {
            itemsControl.setRedraw(true);
        }
        setInfo(getItemsLoadMessage(objectList.size()));
    }

    protected List<OBJECT_TYPE> createViewerInput(Collection<OBJECT_TYPE> objectList) {
        return new ArrayList<>(objectList);
    }

    protected void addExtraColumns(ViewerColumnController<ObjectColumn, Object> columnController, Collection<OBJECT_TYPE> items) {

    }

    public void appendListData(Collection<OBJECT_TYPE> items) {
        setListData(items, true, false);
    }

    public void repackColumns() {
        if (columnController != null) {
            columnController.repackColumns();
        }
    }

    public Collection<OBJECT_TYPE> getListData() {
        return objectList;
    }

    public void clearListData() {
        if (columnController != null) {
            columnController.dispose();
            columnController = null;
        }

        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.setInput(createViewerInput(Collections.emptyList()));
        }
        if (listPropertySource != null) {
            listPropertySource.clearProperties();
        }
        clearLazyCache();
    }

    private void collectItemClasses(OBJECT_TYPE item, List<Class<?>> classList, Map<OBJECT_TYPE, Boolean> collectedSet) {
        if (collectedSet.containsKey(item)) {
            log.warn("Cycled object tree: " + item);
            return;
        }
        collectedSet.put(item, Boolean.TRUE);
        ITreeContentProvider contentProvider = (ITreeContentProvider) itemsViewer.getContentProvider();
        if (!contentProvider.hasChildren(item)) {
            return;
        }
        Object[] children = contentProvider.getChildren(item);
        if (!ArrayUtils.isEmpty(children)) {
            for (Object child : children) {
                OBJECT_TYPE childItem = (OBJECT_TYPE) child;
                Object objectValue = getObjectValue(childItem);
                if (objectValue != null) {
                    if (!classList.contains(objectValue.getClass())) {
                        classList.add(objectValue.getClass());
                    }
                }
                collectItemClasses(childItem, classList, collectedSet);
            }
        }
    }

    protected void resetLazyPropertyCache(OBJECT_TYPE object, String property) {
        synchronized (lazyCache) {
            Map<String, Object> cache = lazyCache.get(object);
            if (cache != null) {
                cache.remove(property);
            }
        }
    }

    private void clearLazyCache() {
        synchronized (lazyCache) {
            lazyCache.clear();
        }
    }

    protected String getItemsLoadMessage(int count) {
        if (count == 0) {
            return UINavigatorMessages.controls_object_list_message_no_items;
        } else {
            return NLS.bind(UINavigatorMessages.controls_object_list_message_items, count);
        }
    }

    public void setDoubleClickHandler(IDoubleClickListener doubleClickHandler) {
        this.doubleClickHandler = doubleClickHandler;
    }

    private Tree getTree() {
        return ((TreeViewer) itemsViewer).getTree();
    }

    private Table getTable() {
        return ((TableViewer) itemsViewer).getTable();
    }

    private synchronized void addLazyObject(OBJECT_TYPE object, ObjectColumn column) {
        if (lazyObjects == null) {
            lazyObjects = new LinkedHashMap<>();
        }
        List<ObjectColumn> objectColumns = lazyObjects.computeIfAbsent(object, k -> new ArrayList<>());
        if (!objectColumns.contains(column)) {
            objectColumns.add(column);
        }
        if (lazyLoadingJob == null) {
            lazyLoadingJob = new LazyLoaderJob();
            lazyLoadingJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    synchronized (ObjectListControl.this) {
                        if (lazyObjects == null || lazyObjects.isEmpty()) {
                            lazyLoadingJob = null;
                        } else {
                            lazyLoadingJob.schedule(LAZY_LOAD_DELAY);
                        }
                    }
                }
            });
            lazyLoadingJob.schedule(LAZY_LOAD_DELAY);
        }
    }

    @Nullable
    private synchronized Map<OBJECT_TYPE, List<ObjectColumn>> obtainLazyObjects() {
        synchronized (lazyCache) {
            if (lazyObjects == null) {
                return null;
            }
            Map<OBJECT_TYPE, List<ObjectColumn>> tmp = lazyObjects;
            lazyObjects = null;
            return tmp;
        }
    }

    @Nullable
    protected final Object getCellValue(Object element, int columnIndex) {
        final ObjectColumn columnInfo = getColumnByIndex(columnIndex);
        if (columnInfo == null) {
            return null;
        }
        return getCellValue(element, columnInfo, true);
    }

    @Nullable
    protected Object getCellValue(Object element, ObjectColumn objectColumn, boolean formatValue) {
        if (element instanceof ObjectsGroupingWrapper) {
            if (objectColumn == groupingColumn) {
                Object groupingKey = ((ObjectsGroupingWrapper) element).groupingKey;
                List<Object> elements = ((ObjectsGroupingWrapper) element).groupedElements;
                int groupElementsSize = 0;
                if (!CommonUtils.isEmpty(elements)) {
                    groupElementsSize = elements.size();
                }
                if (groupingKey == null || "".equals(groupingKey)) {
                    return EMPTY_GROUPING_LABEL + (groupElementsSize > 0 ? " (" + groupElementsSize + ")" : "");
                }
                return groupingKey + (groupElementsSize > 0 ? " (" + groupElementsSize + ")" : "");
            }
            return null;
        }
        OBJECT_TYPE object = (OBJECT_TYPE) element;

        Object objectValue = getObjectValue(object);
        if (objectValue == null) {
            return null;
        }
        ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
        if (prop == null) {
            return null;
        }
        //if (!prop.isReadOnly(objectValue) && isNewObject(object)) {
        // Non-editable properties are empty for new objects
        //return null;
        //}
        if (!isDynamicObject(object) && prop.isLazy(objectValue, true)) {
            synchronized (lazyCache) {
                final Map<String, Object> cache = lazyCache.get(object);
                if (cache != null) {
                    Object value = cache.get(objectColumn.id);
                    if (value != null) {
                        if (value == NULL_VALUE) {
                            return null;
                        } else {
                            if (formatValue) {
                                value = prop.formatValue(object, value);
                            }
                            return value;
                        }
                    }
                }
            }
            if (prop.supportsPreview()) {
                final Object previewValue = getListPropertySource().getPropertyValue(null, objectValue, prop, formatValue);
                if (previewValue != null) {
                    return new LazyValue(previewValue);
                }
            }
            return DEF_LAZY_VALUE;
        }
        return getListPropertySource().getPropertyValue(null, objectValue, prop, formatValue);
    }

    /**
     * Gets property descriptor by column and object value (NB! not OBJECT_TYPE but real object value).
     */
    @Nullable
    private static ObjectPropertyDescriptor getPropertyByObject(@NotNull ObjectColumn column, @Nullable Object objectValue) {
        if (objectValue == null) {
            return null;
        }
        ObjectPropertyDescriptor prop = null;
        for (Class<?> valueClass = objectValue.getClass(); prop == null && valueClass != Object.class; valueClass = valueClass.getSuperclass()) {
            prop = column.propMap.get(valueClass);
        }
        if (prop == null) {
            for (Map.Entry<Class<?>, ObjectPropertyDescriptor> entry : column.propMap.entrySet()) {
                if (entry.getKey().isInstance(objectValue)) {
                    prop = entry.getValue();
                    break;
                }
            }
        }
        return prop;
    }

    @Nullable
    protected Class<?>[] getListBaseTypes(Collection<OBJECT_TYPE> items) {
        return null;
    }

    @Nullable
    public <T> T getSuitableSelectedElement(@NotNull Class<T> adapterType) {
        ISelection selection = getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            T adapter = DBUtils.getAdapter(adapterType, firstElement);
            if (adapter != null) {
                return (T) firstElement;
            }
        }
        return null;
    }

    /**
     * Returns object with properties
     *
     * @param item list item
     * @return object which will be examined for properties
     */
    protected Object getObjectValue(OBJECT_TYPE item) {
        return item;
    }

    protected boolean isDynamicObject(OBJECT_TYPE object) {
        return false;
    }

    /**
     * Returns object's image
     *
     * @param item object
     * @return image or null
     */
    @Nullable
    protected DBPImage getObjectImage(OBJECT_TYPE item) {
        return null;
    }

    protected Color getObjectBackground(OBJECT_TYPE item) {
        return null;
    }

    protected Color getObjectForeground(OBJECT_TYPE item) {
        return null;
    }

    protected boolean isNewObject(OBJECT_TYPE objectValue) {
        return false;
    }

    protected boolean isReadOnlyList() {
        return false;
    }

    @NotNull
    protected Set<DBPPropertyDescriptor> getAllProperties() {
        ObjectColumn[] columns = columnController.getColumnsData(ObjectColumn.class);
        Set<DBPPropertyDescriptor> props = new LinkedHashSet<>();
        for (ObjectColumn column : columns) {
            props.addAll(column.propMap.values());
        }
        return props;
    }

    public void setIsColumnVisibleById(String id, boolean visible) {
        if (columnController != null) {
            ObjectColumn[] columnsData = columnController.getColumnsData(ObjectColumn.class);
            for (int i = 0; i < columnsData.length; i++) {
                if (columnsData[i].id.equals(id)) {
                    columnController.setIsColumnVisible(i, visible);
                }
            }
        }
    }

    protected void createColumn(ObjectPropertyDescriptor prop) {
        ObjectColumn objectColumn = null;
        for (ObjectColumn col : columnController.getColumnsData(ObjectColumn.class)) {
            if (CommonUtils.equalObjects(col.id, prop.getId()) || CommonUtils.equalObjects(col.displayName, prop.getDisplayName())) {
                objectColumn = col;
                break;
            }
        }
        // Use prop class from top parent
        Class<?> propClass = prop.getParent() == null ? prop.getDeclaringClass() : prop.getParent().getDeclaringClass();
        if (objectColumn == null) {

            if (prop.isHidden()) {
                return;
            }
            objectColumn = new ObjectColumn(prop.getId(), prop.getDisplayName());
            objectColumn.addProperty(propClass, prop);

            final CellLabelProvider labelProvider = getColumnLabelProvider(objectColumn);
            final EditingSupport editingSupport = makeEditingSupport(objectColumn);

            // Add column in controller
            columnController.addColumn(
                prop.getDisplayName(),
                prop.getDescription(),
                prop.isNumeric() ? SWT.RIGHT : (prop.isBoolean() ? SWT.CENTER : SWT.NONE),
                prop.isViewable(),
                prop.isNameProperty(),
                prop.isNumeric(),
                objectColumn,
                labelProvider,
                editingSupport);
        } else {
            objectColumn.addProperty(propClass, prop);
//            String oldTitle = objectColumn.item.getText();
//            if (!oldTitle.contains(prop.getDisplayName())) {
//                objectColumn.item.setText(CommonUtils.capitalizeWord(objectColumn.id));
//            }
        }
    }

    //////////////////////////////////////////////////////
    // Overridable functions

    /**
     * Creates service for object loading.
     * @param forUpdate true if it is update/merge operation. I.e. existing object modifications should remain.
     */
    protected abstract LoadingJob<Collection<OBJECT_TYPE>> createLoadService(boolean forUpdate);

    protected ObjectViewerRenderer createRenderer() {
        return new ViewerRenderer();
    }

    //////////////////////////////////////////////////////
    // Edit

    @Nullable
    protected EditingSupport makeEditingSupport(ObjectColumn objectColumn) {
        return null;
    }

    protected void setFocusCell(Object object, ObjectColumn objectColumn) {
        this.focusObject = object;
        this.focusColumn = objectColumn;
    }

    //////////////////////////////////////////////////////
    // Clipboard

    @Override
    public void addClipboardData(CopyMode mode, ClipboardData clipboardData) {
        String selectedText;
        if (mode == CopyMode.ADVANCED) {
            // Multiple rows selected
            // Copy all of them in tsv format
            selectedText = copyGridToText();
            if (!CommonUtils.isEmpty(selectedText)) {
                clipboardData.addTransfer(TextTransfer.getInstance(), selectedText);
            }
        } else {
            IStructuredSelection selection = itemsViewer.getStructuredSelection();
            if (selection.size() > 1) {
                StringBuilder buf = new StringBuilder();
                for (Object o : selection) {
                    Object object = getObjectValue((OBJECT_TYPE) o);

                    ObjectColumn nameColumn = null;
                    int columnsCount = columnController.getColumnsCount();
                    for (int i = 0; i < columnsCount; i++) {
                        ObjectColumn column = getColumnByIndex(i);
                        if (column.isNameColumn(object)) {
                            nameColumn = column;
                            break;
                        }
                    }

                    String objectName = null;
                    if (nameColumn != null) {
                        try {
                            ObjectPropertyDescriptor nameProperty = nameColumn.getProperty(object);
                            if (nameProperty != null) {
                                objectName = CommonUtils.toString(nameProperty.readValue(object, null, true));
                            }
                        } catch (Throwable e) {
                            log.debug(e);
                        }
                    }
                    if (objectName == null) {
                        if (object instanceof DBPNamedObject) {
                            objectName = ((DBPNamedObject) object).getName();
                        } else {
                            objectName = DBValueFormatting.getDefaultValueDisplayString(object, DBDDisplayFormat.UI);
                        }
                    }
                    if (buf.length() > 0) buf.append("\n");
                    if (selection instanceof TreeSelection) {
                        final TreePath[] paths = ((TreeSelection) selection).getPathsFor(o);
                        if (!ArrayUtils.isEmpty(paths)) {
                            final int count = paths[0].getSegmentCount();
                            for (int i = 1; i < count; i++) {
                                buf.append('\t');
                            }
                        }
                    }
                    buf.append(objectName);
                }
                selectedText = buf.toString();
            } else {
                selectedText = getRenderer().getSelectedText();
            }
        }
        if (!CommonUtils.isEmpty(selectedText)) {
            clipboardData.addTransfer(TextTransfer.getInstance(), selectedText);
        }
    }

    //////////////////////////////////////////////////////
    // Editor activation

    private class EditorActivationStrategy extends ColumnViewerEditorActivationStrategy {

        public EditorActivationStrategy(ColumnViewer viewer) {
            super(viewer);
        }

        @Override
        protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            if (ObjectListControl.this.isReadOnlyList()) {
                return false;
            }
            ViewerCell cell = (ViewerCell) event.getSource();
            if (renderer.isHyperlink(cell.getElement(), getCellValue(cell.getElement(), cell.getColumnIndex())) &&
                getItemsViewer().getControl().getCursor() == getItemsViewer().getControl().getDisplay().getSystemCursor(SWT.CURSOR_HAND)) {
                return false;
            }
            return super.isEditorActivationEvent(event);
        }
    }

    private static class EditorActivationListener extends ColumnViewerEditorActivationListener {
        @Override
        public void beforeEditorActivated(ColumnViewerEditorActivationEvent event) {
        }

        @Override
        public void afterEditorActivated(ColumnViewerEditorActivationEvent event) {
        }

        @Override
        public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
        }

        @Override
        public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
        }
    }


    //////////////////////////////////////////////////////
    // Property source implementation

    private class DefaultListPropertySource extends PropertySourceAbstract implements DBNNodeReference {

        DefaultListPropertySource() {
            super(ObjectListControl.this, ObjectListControl.this, true);
        }

        @Override
        public Object getSourceObject() {
            return getCurrentListObject();
        }

        @Override
        public Object getEditableValue() {
            return getObjectValue(getCurrentListObject());
        }

        @Override
        public DBPPropertyDescriptor[] getProperties() {
            return getAllProperties().toArray(new DBPPropertyDescriptor[0]);
        }

        @Override
        public DBNNode getReferencedNode() {
            if (ObjectListControl.this instanceof DBNNodeReference nnc) {
                return nnc.getReferencedNode();
            }
            return null;
        }
    }

    //////////////////////////////////////////////////////
    // Column descriptor

    public static class ObjectColumn {
        String id;
        String displayName;
        int columnIndex;
        Map<Class<?>, ObjectPropertyDescriptor> propMap = new IdentityHashMap<>();

        private ObjectColumn(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        void addProperty(Class<?> objectClass, ObjectPropertyDescriptor prop) {
            this.propMap.put(objectClass, prop);
        }

        public boolean isNameColumn(Object object) {
            final ObjectPropertyDescriptor property = getProperty(object);
            return property != null && property.isNameProperty();
        }

        @Nullable
        public ObjectPropertyDescriptor getProperty(Object object) {
            return object == null ? null : getPropertyByObject(this, object);
        }
    }

    //////////////////////////////////////////////////////
    // List sorter

    protected class ObjectColumnLabelProvider extends ColumnLabelProvider implements ILabelProviderEx {
        private static final int MAX_TOOLTIP_LENGTH = 250;
        protected final ObjectColumn objectColumn;

        protected ObjectColumnLabelProvider(ObjectColumn objectColumn) {
            this.objectColumn = objectColumn;
        }

        @Nullable
        @Override
        public Image getImage(Object element) {
            if (element instanceof ObjectsGroupingWrapper) {
                if (this.objectColumn == groupingColumn) {
                    List<Object> groupedElements = ((ObjectsGroupingWrapper) element).groupedElements;
                    element = groupedElements.get(0);
                } else {
                    return null;
                }
            }
            OBJECT_TYPE object = (OBJECT_TYPE) element;
            final Object objectValue = getObjectValue(object);
            if (objectValue == null) {
                // This may happen if list redraw happens during node dispose
                return null;
            }
            final ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
            if (prop != null && prop.isNameProperty()) {
                DBPImage objectImage = getObjectImage(object);
                return objectImage == null ? null : DBeaverIcons.getImage(objectImage);
            }
            return null;
        }

        @Override
        public String getText(Object element) {
            return getText(element, true);
        }

        @Override
        public Color getBackground(Object element) {
            if (element instanceof ObjectsGroupingWrapper) {
                return null;
            }
            return getObjectBackground((OBJECT_TYPE) element);
        }

        @Override
        public Color getForeground(Object element) {
            if (element instanceof ObjectsGroupingWrapper) {
                return null;
            }
            return getObjectForeground((OBJECT_TYPE) element);
        }

        @Override
        public String getText(Object element, boolean forUI) {
            return getText(element, forUI, false);
        }

        public String getText(Object element, boolean forUI, boolean forTip) {
            Object cellValue = getCellValue(element, objectColumn, forUI);
            if (cellValue instanceof LazyValue) {
                cellValue = ((LazyValue) cellValue).value;
            }
            if (forUI && !sampleItems && renderer.isHyperlink(element, cellValue)) {
                return EMPTY_STRING; //$NON-NLS-1$
            }
            if (element instanceof ObjectsGroupingWrapper) {
                return CommonUtils.toString(cellValue);
            }
            final Object objectValue = getObjectValue((OBJECT_TYPE) element);
            if (objectValue == null) {
                // This may happen if list redraw happens during node dispose
                return EMPTY_STRING;
            }
            final ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
            if (prop != null) {
                if (forUI && cellValue instanceof Boolean) {
                    return EMPTY_STRING;
//                    BooleanRenderer.Style booleanStyle = BooleanRenderer.getDefaultStyle();
//                    if (forTip || !booleanStyle.isText()) {
//                        return EMPTY_STRING;
//                    }
//                    return booleanStyle.getText((Boolean) cellValue);
                }
                if (prop.isPassword() && cellValue instanceof String) {
                    return  CommonUtils.isEmpty((String) cellValue) ? EMPTY_STRING : "************";
                }
                return ObjectViewerRenderer.getCellString(cellValue, prop.isNameProperty());
            } else {
                return EMPTY_STRING;
            }
        }

        @Override
        public String getToolTipText(Object element) {
            String text = getText(element, true, true);
            if (CommonUtils.isEmpty(text)) {
                return null;
            }
            if (text.length() > MAX_TOOLTIP_LENGTH) {
                text = text.substring(0, MAX_TOOLTIP_LENGTH) + "...";
            }
            return text;
        }
    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        private final boolean forUpdate;

        public ObjectsLoadVisualizer(boolean forUpdate) {
            this.forUpdate = forUpdate;
        }

        public ObjectsLoadVisualizer() {
            this(false);
        }

        @Override
        public void completeLoading(Collection<OBJECT_TYPE> items) {
            super.completeLoading(items);
            afterCompleteLoading(items);
        }

        protected void afterCompleteLoading(@NotNull Collection<OBJECT_TYPE> items) {
            setListData(items, false, forUpdate);
        }
    }

    public class ObjectActionVisualizer extends ProgressVisualizer<Void> {

        public ObjectActionVisualizer() {
        }

        @Override
        public void completeLoading(Void v) {
            super.completeLoading(v);
        }
    }

    private static class LazyValue {
        private final Object value;

        private LazyValue(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    class PaintListener implements Listener {

        @Override
        public void handleEvent(Event e) {
            if (isDisposed()) {
                return;
            }
            switch (e.type) {
                case SWT.PaintItem:
                    if (e.index < columnController.getColumnsCount()) {
                        final ObjectColumn objectColumn = getColumnByIndex(e.index);
                        if (objectColumn == null) {
                            return;
                        }
                        final OBJECT_TYPE object = (OBJECT_TYPE) e.item.getData();
                        final boolean isFocusCell = focusObject == object && focusColumn == objectColumn;

                        if (object instanceof ObjectsGroupingWrapper) {
                            break;
                        }
                        final Object objectValue = getObjectValue(object);
                        Object cellValue = getCellValue(object, objectColumn, true);

                        if (cellValue instanceof LazyValue) {
                            if (!lazyLoadCanceled) {
                                addLazyObject(object, objectColumn);
                            }
                        } else {
                            final ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
                            if (itemsViewer.isCellEditorActive() && isFocusCell) {
                                // Do not paint over active editor
                                return;
                            }
                            if (cellValue != null && prop != null) {
                                renderer.paintCell(e, object, cellValue, e.item, prop.getDataType(), e.index, prop.isEditable(objectValue), (e.detail & SWT.SELECTED) == SWT.SELECTED);
                            } else if (prop == null) {
                                renderer.paintInvalidCell(e, e.item, e.index);
                            }
                        }
                        break;
                    }
            }
        }
    }

    private class LazyLoaderJob extends AbstractJob {
        public LazyLoaderJob() {
            super(UINavigatorMessages.controls_object_list_job_props_read);
        }

        @Override
        protected IStatus run(final DBRProgressMonitor monitor) {
            final Map<OBJECT_TYPE, List<ObjectColumn>> objectMap = obtainLazyObjects();
            if (isDisposed()) {
                return Status.OK_STATUS;
            }
            monitor.beginTask(UINavigatorMessages.controls_object_list_monitor_load_lazy_props, objectMap.size());
            for (Map.Entry<OBJECT_TYPE, List<ObjectColumn>> entry : objectMap.entrySet()) {
                if (monitor.isCanceled() || isDisposed()) {
                    break;
                }
                final OBJECT_TYPE element = entry.getKey();
                Object object = getObjectValue(element);
                if (object == null) {
                    continue;
                }
                Map<String, Object> objectCache;
                synchronized (lazyCache) {
                    objectCache = lazyCache.get(element);
                    if (objectCache == null) {
                        objectCache = new HashMap<>();
                        lazyCache.put(element, objectCache);
                    }
                }
                String objectName = GeneralUtils.makeDisplayString(getObjectValue(element)).toString();
                monitor.subTask(NLS.bind(UINavigatorMessages.controls_object_list_monitor_load_props, objectName));
                for (ObjectColumn column : entry.getValue()) {
                    if (monitor.isCanceled() || isDisposed()) {
                        break;
                    }
                    ObjectPropertyDescriptor prop = getPropertyByObject(column, object);
                    if (prop != null) {
                        try {
                            synchronized (lazyCache) {
                                if (objectCache.containsKey(prop.getId())) {
                                    // This property already cached
                                    continue;
                                }
                            }
                            Object lazyValue = prop.readValue(object, monitor, false);
                            if (lazyValue == null) {
                                lazyValue = NULL_VALUE;
                            }
                            synchronized (lazyCache) {
                                objectCache.put(prop.getId(), lazyValue);
                            }
                        } catch (Throwable e) {
                            if (e instanceof InvocationTargetException) {
                                e = ((InvocationTargetException) e).getTargetException();
                            }
                            log.error("Error reading property '" + prop.getId() + "' from " + object, e); //$NON-NLS-1$ //$NON-NLS-2$
                            // do not return error - it causes a lot of error boxes
                            //return RuntimeUtils.makeExceptionStatus(e);
                        }
                    }
                }
                monitor.worked(1);
            }
            monitor.done();
            if (!isDisposed()) {
                // Make refresh of whole table
                // Some other objects could also be updated implicitly with our lazy loader
                UIUtils.asyncExec(() -> {
                    if (!isDisposed()) {
                        itemsViewer.refresh();
                    }
                });
            }
            if (monitor.isCanceled()) {
                lazyLoadCanceled = true;
                obtainLazyObjects();
            }
            return Status.OK_STATUS;
        }
    }

    protected void addColumnConfigAction(IContributionManager contributionManager) {
        // Create lazy action here because columnController might be not instantiated yet
        Action configColumnsAction = new Action(
            UINavigatorMessages.obj_editor_properties_control_action_configure_columns,
            DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            @Override
            public void run() {
                columnController.configureColumns();
            }
        };
        configColumnsAction.setDescription(UINavigatorMessages.obj_editor_properties_control_action_configure_columns_description);
        contributionManager.add(configColumnsAction);
    }

    /**
     * Searcher. Filters elements by name and description
     */
    public class SearcherFilter implements ISearchExecutor {
        @Override
        public boolean performSearch(String searchString, int options) {
            try {
                SearchFilter searchFilter = new SearchFilter(
                    searchString,
                    (options & SEARCH_CASE_SENSITIVE) != 0);
                getItemsViewer().setFilters(new ViewerFilter[]{searchFilter});
                return true;
            } catch (PatternSyntaxException e) {
                log.error(e.getMessage());
                return false;
            }
        }

        @Override
        public void cancelSearch() {
            getItemsViewer().setFilters(new ViewerFilter[]{});
        }
    }

    private static final class SearchFilter extends ViewerFilter {
        private final Pattern pattern;

        private SearchFilter(String searchString, boolean caseSensitiveSearch) {
            pattern = Pattern.compile(SQLUtils.makeLikePattern(searchString), caseSensitiveSearch ? 0 : Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            return (element instanceof DBPNamedObject namedObject && matches(namedObject.getName())) ||
                (element instanceof DBPObjectWithDescription owd && matches(owd.getDescription()));
        }

        private boolean matches(@Nullable CharSequence charSequence) {
            if (charSequence == null) {
                return false;
            }
            Matcher matcher = pattern.matcher(charSequence);
            return matcher.find();
        }
    }

    protected class ViewerRenderer extends ObjectViewerRenderer {
        protected ViewerRenderer() {
            super(itemsViewer);
        }

        @Nullable
        @Override
        public Object getCellValue(Object element, int columnIndex) {
            return ObjectListControl.this.getCellValue(element, columnIndex);
        }
    }

    private String copyGridToText() {
        StringBuilder buf = new StringBuilder();
        int columnsCount = columnController.getColumnsCount();
        {
            // Header
            for (int i = 0; i < columnsCount; i++) {
                ObjectColumn column = getColumnByIndex(i);
                if (i > 0) buf.append("\t");
                buf.append(column.displayName);
            }
            buf.append("\n");
        }
        List<OBJECT_TYPE> elementList = itemsViewer.getStructuredSelection().toList();
        for (OBJECT_TYPE element : elementList) {
            Object object = getObjectValue(element);
            for (int i = 0; i < columnsCount; i++) {
                ObjectPropertyDescriptor property = getColumnByIndex(i).getProperty(object);
                try {
                    Object cellValue = property == null ? null : property.readValue(object, new VoidProgressMonitor(), true);
                    if (i > 0) buf.append("\t");
                    String strValue = DBValueFormatting.getDefaultValueDisplayString(cellValue, DBDDisplayFormat.UI);
                    if (strValue.contains("\n") || strValue.contains("\t")) {
                        strValue = '"' + strValue + '"';
                    }
                    buf.append(strValue);
                } catch (Throwable e) {
                    // ignore
                }
            }
            buf.append("\n");
        }

        return buf.toString();
    }

    private class GroupingViewerColumnController<COLUMN, ELEMENT> extends ViewerColumnController<COLUMN, ELEMENT> {

        private int[] originalColumnOrder;

        GroupingViewerColumnController(String id, ColumnViewer viewer) {
            super(id, viewer);
        }

        @Override
        public void fillConfigMenu(IContributionManager menuManager) {
            super.fillConfigMenu(menuManager);
            if (isTree && supportsDataGrouping()) {
                menuManager.add(new Separator());
                int selectedColumnNumber = columnController.getSelectedColumnNumber();
                String columnName = null;
                final boolean columnPersist = selectedColumnNumber != -1;
                if (columnPersist) {
                    columnName = columnController.getColumnName(selectedColumnNumber);
                }
                menuManager.add(
                    new Action(NLS.bind(UINavigatorMessages.object_list_control_group_by_label, CommonUtils.notEmpty(columnName)), null) {
                        @Override
                        public void run() {
                            if (columnPersist) {
                                groupingColumn = getColumnByIndex(selectedColumnNumber);
                                groupingColumn.columnIndex = selectedColumnNumber;
                                originalColumnOrder = ((TreeViewer) itemsViewer).getTree().getColumnOrder();
                                moveGroupingColumnInTheBeginning(selectedColumnNumber);
                                itemsViewer.setContentProvider(new GroupingTreeProvider());
                                itemsViewer.refresh();
                                ((TreeViewer) itemsViewer).expandToLevel(2);
                            }
                        }

                        @Override
                        public boolean isEnabled() {
                            return columnPersist;
                        }
                    }
                );

                menuManager.add(new Action(UINavigatorMessages.object_list_control_clear_grouping_label, null) {
                    @Override
                    public void run() {
                        groupingColumn = null;
                        restoreOriginalColumnsOrder();
                        itemsViewer.setContentProvider(originalContentProvider);
                        itemsViewer.refresh();
                    }

                    @Override
                    public boolean isEnabled() {
                        return groupingColumn != null;
                    }
                });
            }
        }

        private void moveGroupingColumnInTheBeginning(int groupingColumnPosition) {
            Tree tree = ((TreeViewer) itemsViewer).getTree();
            int[] originalColumnOrder = tree.getColumnOrder();
            int[] newColumnOrder = new int[originalColumnOrder.length];
            newColumnOrder[0] = groupingColumnPosition;
            int originalNumber = 0;
            for (int element : originalColumnOrder) {
                if (element != groupingColumnPosition) {
                    originalNumber++;
                    newColumnOrder[originalNumber] = element;
                }
            }
            tree.setColumnOrder(newColumnOrder);
            columnController.repackColumns();
        }

        private void restoreOriginalColumnsOrder() {
            if (!ArrayUtils.isEmpty(originalColumnOrder)) {
                ((TreeViewer) itemsViewer).getTree().setColumnOrder(originalColumnOrder);
            }
            columnController.repackColumns();
        }
    }

    private class GroupingTreeProvider extends TreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            if (groupingColumn != null) {
                Object[] elements = super.getElements(inputElement);

                if (ArrayUtils.isEmpty(elements)) {
                    return elements;
                }

                int columnIndex = groupingColumn.columnIndex;
                final Map<Object, List<Object>> groups = new HashMap<>();

                for (Object element : elements) {
                    final Object key = getCellValue(element, columnIndex);
                    final List<Object> group = groups.computeIfAbsent(key, x -> new ArrayList<>());
                    group.add(element);
                }

                return groups.entrySet().stream()
                    .map(x -> new ObjectsGroupingWrapper(x.getKey(), x.getValue()))
                    .toArray();
            }
            return super.getElements(inputElement);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (groupingColumn != null && parentElement instanceof ObjectsGroupingWrapper) {
                return ((ObjectsGroupingWrapper) parentElement).groupedElements.toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object element) {
            if (groupingColumn != null) {
                return element instanceof ObjectsGroupingWrapper;
            }
            return false;
        }
    }

    private static class ObjectsGroupingWrapper {
        private final Object groupingKey;
        private final List<Object> groupedElements;

        private ObjectsGroupingWrapper(@Nullable Object groupingKey, @NotNull List<Object> groupedElements) {
            this.groupingKey = groupingKey;
            this.groupedElements = groupedElements;
        }

        @Override
        public String toString() {
            return (groupingKey != null ? "Grouped by: " + groupingKey.toString() + ". " : "") + "Elements amount: " + groupedElements.size();
        }
    }
}
