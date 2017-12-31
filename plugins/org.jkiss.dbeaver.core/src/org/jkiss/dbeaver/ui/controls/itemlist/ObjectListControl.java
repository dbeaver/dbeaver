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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.properties.*;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
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

    private boolean isFitWidth;

    private ColumnViewer itemsViewer;
    //private ColumnViewerEditor itemsEditor;
    private IDoubleClickListener doubleClickHandler;
    private PropertySourceAbstract listPropertySource;

    private ObjectViewerRenderer renderer;
    protected ViewerColumnController columnController;

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

    public ObjectListControl(
        Composite parent,
        int style,
        IContentProvider contentProvider) {
        super(parent, style);
        this.isFitWidth = false;

        int viewerStyle = getDefaultListStyle();
        if ((style & SWT.SHEET) == 0) {
            viewerStyle |= SWT.BORDER;
        }

        EditorActivationStrategy editorActivationStrategy;
        final TraverseListener traverseListener = new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN && doubleClickHandler != null) {
                    doubleClickHandler.doubleClick(new DoubleClickEvent(itemsViewer, itemsViewer.getSelection()));
                    e.doit = false;
                }
            }
        };
        if (contentProvider instanceof ITreeContentProvider) {
            TreeViewer treeViewer = new TreeViewer(this, viewerStyle);
            final Tree tree = treeViewer.getTree();
            tree.setLinesVisible(true);
            tree.setHeaderVisible(true);
            itemsViewer = treeViewer;
            editorActivationStrategy = new EditorActivationStrategy(treeViewer);
            TreeViewerEditor.create(treeViewer, editorActivationStrategy, ColumnViewerEditor.TABBING_CYCLE_IN_ROW);
            // We need measure item listener to prevent collapse/expand on double click
            // Looks like a bug in SWT: http://www.eclipse.org/forums/index.php/t/257325/
            treeViewer.getControl().addListener(SWT.MeasureItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    // Just do nothing
                }
            });
            tree.addTraverseListener(traverseListener);
        } else {
            TableViewer tableViewer = new TableViewer(this, viewerStyle);
            final Table table = tableViewer.getTable();
            table.setLinesVisible(true);
            table.setHeaderVisible(true);
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

        // Add selection listener
        itemsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.isEmpty()) {
                    setCurListObject(null);
                } else {
                    setCurListObject((OBJECT_TYPE) selection.getFirstElement());
                }

                String status;
                if (selection.isEmpty()) {
                    status = ""; //$NON-NLS-1$
                } else if (selection.size() == 1) {
                    Object selectedNode = selection.getFirstElement();
                    status = ObjectViewerRenderer.getCellString(selectedNode, false);
                } else {
                    status = NLS.bind(CoreMessages.controls_object_list_status_objects, selection.size());
                }
                setInfo(status);
            }
        });
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
        return (ObjectColumn) columnController.getColumnData(index);
    }

    public void setFitWidth(boolean fitWidth) {
        isFitWidth = fitWidth;
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
        loadData(true);
    }

    public void loadData(boolean lazy) {
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
                UIUtils.showMessageBox(getShell(), "Load", "Service is busy", SWT.ICON_WARNING);
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
                this.loadingJob = createLoadService();
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
            final LoadingJob<Collection<OBJECT_TYPE>> loadService = createLoadService();
            if (loadService != null) {
                loadService.syncRun();
            }
        }
    }

    protected int getDataLoadTimeout() {
        return 4000;
    }

    protected void setListData(Collection<OBJECT_TYPE> items, boolean append) {
        final Control itemsControl = itemsViewer.getControl();
        if (itemsControl.isDisposed()) {
            return;
        }

        itemsControl.setRedraw(false);
        try {
            final boolean reload = !append && (objectList == null) || (columnController == null);

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
                        if (object != null && !classList.contains(object.getClass())) {
                            // Remove all base classes if we have sub class
                            // But keep interfaces because we may have multiple implementations of e.g. DBPNamedObject
                            // and we need to show "Name" instead of particular name props
                            for (int i = 0; i < classList.size(); i++) {
                                Class<?> c = classList.get(i);
                                if (!c.isInterface() && c.isAssignableFrom(object.getClass())) {
                                    classList.remove(i);
                                } else {
                                    i++;
                                }
                            }
                            classList.add(object.getClass());
                        }
                        if (renderer.isTree()) {
                            Map<OBJECT_TYPE, Boolean> collectedSet = new IdentityHashMap<>();
                            collectItemClasses(item, classList, collectedSet);
                        }
                    }
                }

                IPropertyFilter propertyFilter = new DataSourcePropertyFilter(
                    ObjectListControl.this instanceof IDataSourceContainerProvider ?
                        ((IDataSourceContainerProvider) ObjectListControl.this).getDataSourceContainer() :
                        null);

                // Collect all properties
                List<ObjectPropertyDescriptor> allProps = ObjectAttributeDescriptor.extractAnnotations(getListPropertySource(), classList, propertyFilter);

                if (reload) {
                    clearListData();
                    columnController = new ViewerColumnController(getListConfigId(classList), getItemsViewer());
                }

                // Create columns from classes' annotations
                for (ObjectPropertyDescriptor prop : allProps) {
                    if (!getListPropertySource().hasProperty(prop)) {
                        getListPropertySource().addProperty(prop);
                        createColumn(prop);
                    }
                }
            }

            if (itemsControl.isDisposed()) {
                return;
            }
            if (reload) {
                columnController.createColumns(false);
            }
            if (reload || objectList.isEmpty()) {
                // Set viewer content
                objectList = CommonUtils.isEmpty(items) ? new ArrayList<OBJECT_TYPE>() : new ArrayList<>(items);

                // Pack columns
                sampleItems = true;
                try {
                    List<OBJECT_TYPE> sampleList;
                    if (objectList.size() > 200) {
                        sampleList = objectList.subList(0, 100);
                    } else {
                        sampleList = objectList;
                    }
                    itemsViewer.setInput(sampleList);

                    if (renderer.isTree()) {
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
                    if (!objectList.equals(items)) {
                        int newListSize = items.size();
                        int itemIndex = 0;
                        for (OBJECT_TYPE newObject : items) {
                            if (itemIndex >= objectList.size()) {
                                // Add to tail
                                objectList.add(itemIndex, newObject);
                            } else {
                                OBJECT_TYPE oldObject = objectList.get(itemIndex);
                                if (!CommonUtils.equalObjects(oldObject, newObject)) {
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

    public void appendListData(Collection<OBJECT_TYPE> items) {
        setListData(items, true);
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
            itemsViewer.setInput(Collections.emptyList());
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

    private void clearLazyCache() {
        synchronized (lazyCache) {
            lazyCache.clear();
        }
    }

    protected String getItemsLoadMessage(int count) {
        if (count == 0) {
            return CoreMessages.controls_object_list_message_no_items;
        } else {
            return NLS.bind(CoreMessages.controls_object_list_message_items, count);
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
        List<ObjectColumn> objectColumns = lazyObjects.get(object);
        if (objectColumns == null) {
            objectColumns = new ArrayList<>();
            lazyObjects.put(object, objectColumns);
        }
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
        return getCellValue(element, columnInfo);
    }

    @Nullable
    protected Object getCellValue(Object element, ObjectColumn objectColumn) {
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
        if (prop.isLazy(objectValue, true)) {
            synchronized (lazyCache) {
                final Map<String, Object> cache = lazyCache.get(object);
                if (cache != null) {
                    final Object value = cache.get(objectColumn.id);
                    if (value != null) {
                        if (value == NULL_VALUE) {
                            return null;
                        } else {
                            return value;
                        }
                    }
                }
            }
            if (prop.supportsPreview()) {
                final Object previewValue = getListPropertySource().getPropertyValue(null, objectValue, prop);
                if (previewValue != null) {
                    return new LazyValue(previewValue);
                }
            }
            return DEF_LAZY_VALUE;
        }
        return getListPropertySource().getPropertyValue(null, objectValue, prop);
    }

    /**
     * Gets property descriptor by column and object value (NB! not OBJECT_TYPE but real object value).
     */
    @Nullable
    private static ObjectPropertyDescriptor getPropertyByObject(ObjectColumn column, Object objectValue) {
        ObjectPropertyDescriptor prop = null;
        for (Class valueClass = objectValue.getClass(); prop == null && valueClass != Object.class; valueClass = valueClass.getSuperclass()) {
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

    /**
     * Returns object with properties
     *
     * @param item list item
     * @return object which will be examined for properties
     */
    protected Object getObjectValue(OBJECT_TYPE item) {
        return item;
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

    @NotNull
    protected Set<DBPPropertyDescriptor> getAllProperties() {
        ObjectColumn[] columns = columnController.getColumnsData(ObjectColumn.class);
        Set<DBPPropertyDescriptor> props = new LinkedHashSet<>();
        for (ObjectColumn column : columns) {
            props.addAll(column.propMap.values());
        }
        return props;
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
                prop.isNumeric() ? SWT.RIGHT : SWT.NONE,
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

    protected abstract LoadingJob<Collection<OBJECT_TYPE>> createLoadService();

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
                for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
                    Object object = getObjectValue((OBJECT_TYPE) iter.next());

                    ObjectColumn nameColumn = null;
                    int columnsCount = columnController.getColumnsCount();
                    for (int i = 0 ; i < columnsCount; i++) {
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
                                objectName = CommonUtils.toString(nameProperty.readValue(object, null));
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
            ViewerCell cell = (ViewerCell) event.getSource();
            if (renderer.isHyperlink(getCellValue(cell.getElement(), cell.getColumnIndex())) &&
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

    private class DefaultListPropertySource extends PropertySourceAbstract {

        public DefaultListPropertySource() {
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
        public DBPPropertyDescriptor[] getPropertyDescriptors2() {
            Set<DBPPropertyDescriptor> props = getAllProperties();
            return props.toArray(new DBPPropertyDescriptor[props.size()]);
        }

    }

    //////////////////////////////////////////////////////
    // Column descriptor

    protected static class ObjectColumn {
        String id;
        String displayName;
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
        protected final ObjectColumn objectColumn;

        protected ObjectColumnLabelProvider(ObjectColumn objectColumn) {
            this.objectColumn = objectColumn;
        }

        @Nullable
        @Override
        public Image getImage(Object element) {
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
            final Color background = getObjectBackground((OBJECT_TYPE) element);
            if (background == null) {

            }
            return background;
        }

        @Override
        public Color getForeground(Object element) {
            return getObjectForeground((OBJECT_TYPE) element);
        }

        @Override
        public String getText(Object element, boolean forUI) {
            Object cellValue = getCellValue(element, objectColumn);
            if (cellValue instanceof LazyValue) {
                cellValue = ((LazyValue) cellValue).value;
            }
            if (forUI && !sampleItems && renderer.isHyperlink(cellValue)) {
                return ""; //$NON-NLS-1$
            }
            final Object objectValue = getObjectValue((OBJECT_TYPE) element);
            if (objectValue == null) {
                // This may happen if list redraw happens during node dispose
                return "";
            }
            final ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
            if (prop != null) {
                if (forUI && cellValue instanceof Boolean) {
                    return "";
                }
                return ObjectViewerRenderer.getCellString(cellValue, prop.isNameProperty());
            } else {
                return "";
            }
        }
    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        public ObjectsLoadVisualizer() {
        }

        @Override
        public void completeLoading(Collection<OBJECT_TYPE> items) {
            super.completeLoading(items);
            setListData(items, false);
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
                        final OBJECT_TYPE object = (OBJECT_TYPE) e.item.getData();
                        final boolean isFocusCell = focusObject == object && focusColumn == objectColumn;

                        final Object objectValue = getObjectValue(object);
                        Object cellValue = getCellValue(object, objectColumn);

                        if (cellValue instanceof LazyValue) {
                            if (!lazyLoadCanceled) {
                                addLazyObject(object, objectColumn);
                            }
                        } else if (cellValue != null) {
                            ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
                            if (prop != null) {
                                if (itemsViewer.isCellEditorActive() && isFocusCell) {
                                    // Do not paint over active editor
                                    return;
                                }
                                renderer.paintCell(e, object, e.item, e.index, prop.isEditable(objectValue), (e.detail & SWT.SELECTED) == SWT.SELECTED);
                            }
                        }
                        break;
                    }
            }
        }
    }

    private class LazyLoaderJob extends AbstractJob {
        public LazyLoaderJob() {
            super(CoreMessages.controls_object_list_job_props_read);
        }

        @Override
        protected IStatus run(final DBRProgressMonitor monitor) {
            final Map<OBJECT_TYPE, List<ObjectColumn>> objectMap = obtainLazyObjects();
            if (isDisposed()) {
                return Status.OK_STATUS;
            }
            monitor.beginTask(CoreMessages.controls_object_list_monitor_load_lazy_props, objectMap.size());
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
                monitor.subTask(NLS.bind(CoreMessages.controls_object_list_monitor_load_props, objectName));
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
                            Object lazyValue = prop.readValue(object, monitor);
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
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!isDisposed()) {
                            itemsViewer.refresh();
                        }
                    }
                });
            }

/*
            // Update viewer
            if (!isDisposed()) {
                getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        itemsViewer.getControl().setRedraw(false);
                        try {
                            itemsViewer.update(objectMap.keySet().toArray(), null);
                        } finally {
                            itemsViewer.getControl().setRedraw(true);
                        }
                    }
                });
            }
*/
            if (monitor.isCanceled()) {
                lazyLoadCanceled = true;
                obtainLazyObjects();
            }
            return Status.OK_STATUS;
        }
    }

    protected void addColumnConfigAction(IContributionManager contributionManager) {
        Action configColumnsAction = new Action(
            CoreMessages.obj_editor_properties_control_action_configure_columns,
            DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            @Override
            public void run() {
                columnController.configureColumns();
            }
        };
        configColumnsAction.setDescription(CoreMessages.obj_editor_properties_control_action_configure_columns_description);
        contributionManager.add(configColumnsAction);
    }

    /**
     * Searcher. Filters elements by name
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

    private class SearchFilter extends ViewerFilter {
        final Pattern pattern;

        public SearchFilter(String searchString, boolean caseSensitiveSearch) throws PatternSyntaxException {
            pattern = Pattern.compile(SQLUtils.makeLikePattern(searchString), caseSensitiveSearch ? 0 : Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof DBNNode) {
                return pattern.matcher(((DBNNode) element).getName()).find();
            }
            return false;
        }
    }

    /**
     * Legacy searcher. Highlights foudn elements
     */
    protected class SearcherHighligther extends ObjectSearcher<DBNNode> {
        @Override
        protected void setInfo(String message)
        {
            ObjectListControl.this.setInfo(message);
        }

        @Override
        protected Collection<DBNNode> getContent()
        {
            return (Collection<DBNNode>) getItemsViewer().getInput();
        }

        @Override
        protected void selectObject(DBNNode object)
        {
            getItemsViewer().setSelection(object == null ? new StructuredSelection() : new StructuredSelection(object));
        }

        @Override
        protected void updateObject(DBNNode object)
        {
            getItemsViewer().update(object, null);
        }

        @Override
        protected void revealObject(DBNNode object)
        {
            getItemsViewer().reveal(object);
        }

    }


    protected class ViewerRenderer extends ObjectViewerRenderer {
        protected ViewerRenderer() {
            super(itemsViewer);
        }

        @Nullable
        @Override
        protected Object getCellValue(Object element, int columnIndex) {
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
                    Object cellValue = property == null ? null : property.readValue(object, new VoidProgressMonitor());
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

}
