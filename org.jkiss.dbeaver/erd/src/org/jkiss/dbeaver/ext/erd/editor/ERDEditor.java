/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.action.SchemaContextMenuProvider;
import org.jkiss.dbeaver.ext.erd.directedit.StatusLineValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.dnd.DataEditDropTargetListener;
import org.jkiss.dbeaver.ext.erd.dnd.DataElementFactory;
import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Relationship;
import org.jkiss.dbeaver.ext.erd.model.Schema;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Editor implementation based on the the example editor skeleton that is built in <i>Building
 * an editor </i> in chapter <i>Introduction to GEF </i>
 */
public class ERDEditor extends GraphicalEditorWithFlyoutPalette
		implements
			CommandStackListener,
			ISelectionListener,
        IDatabaseObjectEditor<IDatabaseObjectManager<DBSEntityContainer>>
{
    static final Log log = LogFactory.getLog(ERDEditor.class);

	private Schema schema;

    private ProgressControl progressControl;

	/** the undoable <code>IPropertySheetPage</code> */
	private PropertySheetPage undoablePropertySheetPage;

	/** the graphical viewer */
	private GraphicalViewer graphicalViewer;

	/** the list of action ids that are to EditPart actions */
	private List<String> editPartActionIDs = new ArrayList<String>();

	/** the list of action ids that are to CommandStack actions */
	private List<String> stackActionIDs = new ArrayList<String>();

	/** the list of action ids that are editor actions */
	private List<String> editorActionIDs = new ArrayList<String>();

	/** the overview outline page */
	private ERDOutlinePage outlinePage;

	/** the editor's action registry */
	private ActionRegistry actionRegistry;

	/** the <code>EditDomain</code> */
	private DefaultEditDomain editDomain;

	/** the dirty state */
	private boolean isDirty;

    private DBSEntityContainer entityContainer;
    private boolean isReadOnly;
    private boolean isLoaded;
	/**
	 * No-arg constructor
	 */
	public ERDEditor()
	{
	}

	/**
	 * Initializes the editor.
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
        // Editor is readonly if editor input is not a file but database object
        this.isReadOnly = !(input instanceof IFileEditorInput);

        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);

		// store site and input
		setSite(site);
		setInput(input);

		// add CommandStackListener
		getCommandStack().addCommandStackListener(this);

		// add selection change listener
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

		// initialize actions
		createActions();

	}

    @Override
    public void createPartControl(Composite parent) {
        progressControl = new ProgressControl(parent, SWT.NONE, this.getSite().getPart());

        super.createPartControl(progressControl.createContentContainer());

        progressControl.createProgressPanel();
    }

    /** the selection listener implementation */
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		updateActions(editPartActionIDs);
	}

	/**
	 * The <code>CommandStackListener</code> that listens for
	 * <code>CommandStack </code> changes.
	 */
	public void commandStackChanged(EventObject event)
	{
		updateActions(stackActionIDs);
		setDirty(getCommandStack().isDirty());
	}

	/**
	 * Returns the <code>GraphicalViewer</code> of this editor.
	 * 
	 * @return the <code>GraphicalViewer</code>
	 */
	public GraphicalViewer getGraphicalViewer()
	{
		return graphicalViewer;
	}

	public void dispose()
	{
        if (progressControl != null && !progressControl.isDisposed()) {
            progressControl.dispose();
        }
		// remove CommandStackListener
		getCommandStack().removeCommandStackListener(this);
		// remove selection listener
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		// dispos3 the ActionRegistry (will dispose all actions)
		getActionRegistry().dispose();
		// important: always call super implementation of dispose
		super.dispose();
	}

	/**
	 * Adaptable implementation for Editor
	 */
	public Object getAdapter(Class adapter)
	{
		// we need to handle common GEF elements we created
		if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class)
			return getGraphicalViewer();
		else if (adapter == CommandStack.class)
			return getCommandStack();
		else if (adapter == EditDomain.class)
			return getEditDomain();
		else if (adapter == ActionRegistry.class)
			return getActionRegistry();
		else if (adapter == IPropertySheetPage.class)
			return getPropertySheetPage();
		else if (adapter == IContentOutlinePage.class)
			return getOverviewOutlinePage();

		// the super implementation handles the rest
		return super.getAdapter(adapter);
	}

	/**
	 * Saves the schema model to the file
	 * 
	 * @see EditorPart#doSave
	 */
	public void doSave(IProgressMonitor monitor)
	{
		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream objectOut = new ObjectOutputStream(out);
			objectOut.writeObject(schema);
			objectOut.close();
            IEditorInput input = getEditorInput();
            if (input instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) input).getFile();
                try {
                    file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor);
                } finally {
                    out.close();
                }
            }
		}
		catch (Exception e)
		{
			log.error("Could not save diagram", e);
		}
		getCommandStack().markSaveLocation();
	}

	/**
	 * Save as not allowed
	 */
	public void doSaveAs()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Save as not allowed
	 */
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	/**
	 * Indicates if the editor has unsaved changes.
	 * 
	 * @see EditorPart#isDirty
	 */
	public boolean isDirty()
	{
		return !isReadOnly && isDirty;
	}

	/**
	 * Returns the <code>CommandStack</code> of this editor's
	 * <code>EditDomain</code>.
	 * 
	 * @return the <code>CommandStack</code>
	 */
	public CommandStack getCommandStack()
	{
		return getEditDomain().getCommandStack();
	}

	/**
	 * Returns the schema model associated with the editor
	 * 
	 * @return an instance of <code>Schema</code>
	 */
	public Schema getSchema()
	{
		return schema;
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);

        if (input instanceof IFileEditorInput) {
            loadContentFromFile((IFileEditorInput) input);
        } else {
            // Setup empty schema for now
            // Actual data will be loaded later in activatePart
            schema = new Schema("empty");
        }
	}

    private Schema loadFromDatabase(DBRProgressMonitor monitor)
        throws DBException
    {
        if (entityContainer == null) {
            log.error("Database object must be entity container to render ERD diagram");
            return null;
        }
        schema = new Schema(entityContainer.getName());

        // Cache structure
        entityContainer.cacheStructure(monitor, DBSEntityContainer.STRUCT_ENTITIES | DBSEntityContainer.STRUCT_ASSOCIATIONS | DBSEntityContainer.STRUCT_ATTRIBUTES);

        // Load entities
        Map<DBSObject, Table> tableMap = new HashMap<DBSObject, Table>();
        Collection<? extends DBSObject> entities = entityContainer.getChildren(monitor);
        for (DBSObject entity : entities) {
            Table table = new Table(entity.getName(), schema);

            if (entity instanceof DBSTable) {
                Collection<? extends DBSTableColumn> columns = ((DBSTable) entity).getColumns(monitor);
                for (DBSTableColumn column : columns) {
                    Column c1 = new Column(column.getName(), column.getTypeName());
                    table.addColumn(c1);
                }
            }
            schema.addTable(table);
            tableMap.put(entity, table);
        }

        // Load relations
        for (DBSObject entity : entities) {
            Table table1 = tableMap.get(entity);

            if (entity instanceof DBSTable) {
                try {
                    Collection<? extends DBSForeignKey> fks = ((DBSTable) entity).getForeignKeys(monitor);
                    for (DBSForeignKey fk : fks) {
                        Table table2 = tableMap.get(fk.getReferencedKey().getTable());
                        if (table2 == null) {
                            log.warn("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                        } else {
                            if (table1 != table2) {
                                Relationship relationship = new Relationship(table2, table1);
                            }
                        }
                    }
                } catch (DBException e) {
                    log.warn("Could not load entity '" + entity.getName() + "' foreign keys", e);
                }
            }
        }

        return schema;
    }

    private void loadContentFromFile(IFileEditorInput input) {
        IFile file = input.getFile();
        try
        {
            setPartName(file.getName());
            InputStream is = file.getContents(true);
            ObjectInputStream ois = new ObjectInputStream(is);
            schema = (Schema) ois.readObject();
            ois.close();
        }
        catch (Exception e)
        {
            log.error("Error loading diagram from file '" + file.getFullPath().toString() + "'", e);
            schema = getContent();
        }
    }

    /**
	 * Creates a PaletteViewerProvider that will be used to create palettes for
	 * the view and the flyout.
	 * 
	 * @return the palette provider
	 */
	protected PaletteViewerProvider createPaletteViewerProvider()
	{
		return new SchemaPaletteViewerProvider(editDomain);
	}
	

	/**
	 * Creates a new <code>GraphicalViewer</code>, configures, registers and
	 * initializes it.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	protected void createGraphicalViewer(Composite parent)
	{
		GraphicalViewer viewer = createViewer(parent);

		GraphicalViewerKeyHandler graphicalViewerKeyHandler = new GraphicalViewerKeyHandler(viewer);
		KeyHandler parentKeyHandler = graphicalViewerKeyHandler.setParent(getCommonKeyHandler());
		viewer.setKeyHandler(parentKeyHandler);

		// hook the viewer into the EditDomain
		getEditDomain().addViewer(viewer);

		// activate the viewer as selection provider for Eclipse
		getSite().setSelectionProvider(viewer);

		viewer.setContents(schema);

		ContextMenuProvider provider = new SchemaContextMenuProvider(viewer, getActionRegistry());
		viewer.setContextMenu(provider);
		getSite().registerContextMenu("org.jkiss.dbeaver.ext.erd.editor.contextmenu", provider, viewer);

		this.graphicalViewer = viewer;

	}

    private GraphicalViewer createViewer(Composite parent) {
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(getEditorSite());
        GraphicalViewer viewer = new ValidationEnabledGraphicalViewer(validationMessageHandler);
        viewer.createControl(parent);

        // configure the viewer
        viewer.getControl().setBackground(ColorConstants.white);
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));

        // initialize the viewer with input
        viewer.setEditPartFactory(new ERDEditPartFactory());

        return viewer;
    }

    protected KeyHandler getCommonKeyHandler()
	{

		KeyHandler sharedKeyHandler = new KeyHandler();
		sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(
				ActionFactory.DELETE.getId()));
		sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(
				GEFActionConstants.DIRECT_EDIT));

		return sharedKeyHandler;
	}

	/**
	 * Sets the dirty state of this editor.
	 * 
	 * <p>
	 * An event will be fired immediately if the new state is different than the
	 * current one.
	 * 
	 * @param dirty
	 *            the new dirty state to set
	 */
	protected void setDirty(boolean dirty)
	{
		if (isDirty != dirty)
		{
			isDirty = dirty;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	/**
	 * Creates actions and registers them to the ActionRegistry.
	 */
	protected void createActions()
	{
		addStackAction(new UndoAction(this));
		addStackAction(new RedoAction(this));
		addEditPartAction(new DeleteAction((IWorkbenchPart) this));
		addEditorAction(new SaveAction(this));
		addEditorAction(new PrintAction(this));
	}

	/**
	 * Adds an <code>EditPart</code> action to this editor.
	 * 
	 * <p>
	 * <code>EditPart</code> actions are actions that depend and work on the
	 * selected <code>EditPart</code>s.
	 * 
	 * @param action
	 *            the <code>EditPart</code> action
	 */
	protected void addEditPartAction(SelectionAction action)
	{
		getActionRegistry().registerAction(action);
		editPartActionIDs.add(action.getId());
	}

	/**
	 * Adds an <code>CommandStack</code> action to this editor.
	 * 
	 * <p>
	 * <code>CommandStack</code> actions are actions that depend and work on
	 * the <code>CommandStack</code>.
	 * 
	 * @param action
	 *            the <code>CommandStack</code> action
	 */
	protected void addStackAction(StackAction action)
	{
		getActionRegistry().registerAction(action);
		stackActionIDs.add(action.getId());
	}

	/**
	 * Adds an editor action to this editor.
	 * 
	 * <p>
	 * <Editor actions are actions that depend and work on the editor.
	 * 
	 * @param action
	 *            the editor action
	 */
	protected void addEditorAction(WorkbenchPartAction action)
	{
		getActionRegistry().registerAction(action);
		editorActionIDs.add(action.getId());
	}

	/**
	 * Adds an action to this editor's <code>ActionRegistry</code>. (This is
	 * a helper method.)
	 * 
	 * @param action
	 *            the action to add.
	 */
	protected void addAction(IAction action)
	{
		getActionRegistry().registerAction(action);
	}

	/**
	 * Updates the specified actions.
	 * 
	 * @param actionIds
	 *            the list of ids of actions to update
	 */
	protected void updateActions(List actionIds)
	{
		for (Iterator ids = actionIds.iterator(); ids.hasNext();)
		{
			IAction action = getActionRegistry().getAction(ids.next());
			if (null != action && action instanceof UpdateAction)
				((UpdateAction) action).update();

		}
	}

	/**
	 * Returns the action registry of this editor.
	 * 
	 * @return the action registry
	 */
	protected ActionRegistry getActionRegistry()
	{
		if (actionRegistry == null)
			actionRegistry = new ActionRegistry();

		return actionRegistry;
	}

	/**
	 * Returns the overview for the outline view.
	 * 
	 * @return the overview
	 */
	protected ERDOutlinePage getOverviewOutlinePage()
	{
		if (null == outlinePage && null != getGraphicalViewer())
		{
			RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
			if (rootEditPart instanceof ScalableFreeformRootEditPart)
			{
				outlinePage = new ERDOutlinePage((ScalableFreeformRootEditPart) rootEditPart);
			}
		}

		return outlinePage;
	}

	/**
	 * Returns the undoable <code>PropertySheetPage</code> for this editor.
	 * 
	 * @return the undoable <code>PropertySheetPage</code>
	 */
	protected PropertySheetPage getPropertySheetPage()
	{
		if (null == undoablePropertySheetPage)
		{
			undoablePropertySheetPage = new PropertySheetPage();
			undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
		}

		return undoablePropertySheetPage;
	}

	/*
	 */
	protected void firePropertyChange(int propertyId)
	{
		super.firePropertyChange(propertyId);
		updateActions(editorActionIDs);
	}

	/**
	 * @return the preferences for the Palette Flyout
	 */
	protected FlyoutPreferences getPalettePreferences()
	{
		return new PaletteFlyoutPreferences();
	}

	/**
	 * @return the PaletteRoot to be used with the PaletteViewer
	 */
	protected PaletteRoot getPaletteRoot()
	{
		return createPaletteRoot();
	}

	/**
	 * Returns the content of this editor
	 * 
	 * @return the model object
	 */
	private Schema getContent()
	{
		return new Schema("Schema");//ContentCreator().getContent();
	}

    public void initObjectEditor(IDatabaseObjectManager<DBSEntityContainer> manager)
    {
        entityContainer = manager.getObject();
    }

    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        LoadingUtils.executeService(
            new AbstractLoadService<Schema>("Load schema") {
                public Schema evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        return loadFromDatabase(getProgressMonitor());
                    }
                    catch (DBException e) {
                        log.error(e);
                    }

                    isLoaded = true;
                    return null;
                }
            },
            progressControl.createLoadVisualizer());

    }

    public void deactivatePart() {

    }

    public PaletteRoot createPaletteRoot()
    {
        // create root
        PaletteRoot paletteRoot = new PaletteRoot();

        // a group of default control tools
        PaletteGroup controls = new PaletteGroup("Controls");
        paletteRoot.add(controls);

        // the selection tool
        ToolEntry selectionTool = new SelectionToolEntry();
        controls.add(selectionTool);

        // use selection tool as default entry
        paletteRoot.setDefaultEntry(selectionTool);

        // the marquee selection tool
        controls.add(new MarqueeToolEntry());

        // a separator
        PaletteSeparator separator = new PaletteSeparator(Activator.PLUGIN_ID + ".palette.separator");
        separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
        controls.add(separator);

        if (!isReadOnly) {
            controls.add(new ConnectionCreationToolEntry("Connections", "Create Connections", null,
                Activator.getImageDescriptor("icons/relationship.gif"),
                Activator.getImageDescriptor("icons/relationship.gif")));

            PaletteDrawer drawer = new PaletteDrawer("New Component",
                Activator.getImageDescriptor("icons/connection.gif"));

            List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

            CombinedTemplateCreationEntry tableEntry = new CombinedTemplateCreationEntry("New Table", "Create a new table",
                    Table.class, new DataElementFactory(Table.class),
                    Activator.getImageDescriptor("icons/table.gif"),
                    Activator.getImageDescriptor("icons/table.gif"));

            CombinedTemplateCreationEntry columnEntry = new CombinedTemplateCreationEntry("New Column", "Add a new column",
                    Column.class, new DataElementFactory(Column.class),
                    Activator.getImageDescriptor("icons/column.gif"),
                    Activator.getImageDescriptor("icons/column.gif"));

            entries.add(tableEntry);
            entries.add(columnEntry);

            drawer.addAll(entries);

            paletteRoot.add(drawer);
        }

        return paletteRoot;

    }

    private class ProgressControl extends ProgressPageControl {
        private ToolItem itemZoomIn;
        private ToolItem itemZoomOut;
        private ToolItem itemZoomNorm;
        private ToolItem itemRefresh;

        private ProgressControl(Composite parent, int style, IWorkbenchPart workbenchPart) {
            super(parent, style, workbenchPart);
        }

        @Override
        public void dispose() {
            UIUtils.dispose(itemZoomIn);
            UIUtils.dispose(itemZoomOut);
            UIUtils.dispose(itemZoomNorm);
            UIUtils.dispose(itemRefresh);
            super.dispose();
        }

        protected int getProgressCellCount()
        {
            return 3;
        }

        @Override
        protected Composite createProgressPanel(Composite container) {
            Composite infoGroup = super.createProgressPanel(container);

            ToolBar toolBar = new ToolBar(infoGroup, SWT.FLAT | SWT.HORIZONTAL);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            toolBar.setLayoutData(gd);
            itemZoomIn = UIUtils.createToolItem(toolBar, "Zoom In", DBIcon.ZOOM_IN, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
/*
                    synchronized (ERDEditor.this) {
                        if (graph.getScale() < 2.0) {
                            graph.setScale(graph.getScale() + 0.1);
                        }
                    }
*/
                }
            });
            itemZoomOut = UIUtils.createToolItem(toolBar, "Zoom Out", DBIcon.ZOOM_OUT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
/*
                    synchronized (ERDEditor.this) {
                        if (graph.getScale() > 0.2) {
                            graph.setScale(graph.getScale() - 0.1);
                        }
                    }
*/
                }
            });
            itemZoomNorm = UIUtils.createToolItem(toolBar, "Standard Zoom", DBIcon.ZOOM, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
/*
                    synchronized (ERDEditor.this) {
                        graph.setScale(1.0);
                    }
*/
                }
            });
            itemRefresh = UIUtils.createToolItem(toolBar, "Refresh", DBIcon.RS_REFRESH, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {

                }
            });
            return infoGroup;
        }

        public ProgressVisualizer<Schema> createLoadVisualizer() {
            return new LoadVisualizer();
        }

        private class LoadVisualizer extends ProgressVisualizer<Schema> {
            @Override
            public void completeLoading(Schema schema) {
                super.completeLoading(schema);
                graphicalViewer.setContents(schema);
            }
        }

    }

}