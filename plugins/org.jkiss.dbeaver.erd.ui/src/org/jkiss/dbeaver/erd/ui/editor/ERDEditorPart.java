/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PrintFigureOperation;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.action.DiagramLayoutAction;
import org.jkiss.dbeaver.erd.ui.action.DiagramToggleGridAction;
import org.jkiss.dbeaver.erd.ui.action.DiagramToggleHandAction;
import org.jkiss.dbeaver.erd.ui.action.ERDEditorPropertyTester;
import org.jkiss.dbeaver.erd.ui.directedit.StatusLineValidationMessageHandler;
import org.jkiss.dbeaver.erd.ui.dnd.DataEditDropTargetListener;
import org.jkiss.dbeaver.erd.ui.dnd.NodeDropTargetListener;
import org.jkiss.dbeaver.erd.ui.editor.tools.ChangeZOrderAction;
import org.jkiss.dbeaver.erd.ui.editor.tools.ResetPartColorAction;
import org.jkiss.dbeaver.erd.ui.editor.tools.SetPartColorAction;
import org.jkiss.dbeaver.erd.ui.editor.tools.SetPartSettingsAction;
import org.jkiss.dbeaver.erd.ui.export.ERDExportFormatHandler;
import org.jkiss.dbeaver.erd.ui.export.ERDExportFormatRegistry;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.ERDContainerDecorated.NodeVisualInfo;
import org.jkiss.dbeaver.erd.ui.model.ERDContentProviderDecorated;
import org.jkiss.dbeaver.erd.ui.model.ERDDecorator;
import org.jkiss.dbeaver.erd.ui.model.ERDDecoratorDefault;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationDescriptor;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationRegistry;
import org.jkiss.dbeaver.erd.ui.part.*;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouterDescriptor;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouterRegistry;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceTask;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseModellerEditor;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.actions.ToggleViewAction;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Editor implementation based on the example editor skeleton that is built in <i>Building
 * an editor </i> in chapter <i>Introduction to .gef </i>
 */
public abstract class ERDEditorPart extends GraphicalEditorWithFlyoutPalette
        implements DBPDataSourceTask, IDatabaseModellerEditor, ISearchContextProvider, IRefreshablePart, INavigatorModelView {
    private static final Log searcherLog = Log.getLog(Searcher.class);

    @Nullable
    protected ProgressControl progressControl;

    /**
     * the undoable <code>IPropertySheetPage</code>
     */
    private PropertySheetPage undoablePropertySheetPage;

    /**
     * the graphical viewer
     */
    private ScalableFreeformRootEditPart rootPart;

    /**
     * the list of action ids that are to EditPart actions
     */
    private List<String> editPartActionIDs = new ArrayList<>();

    /**
     * the overview outline page
     */
    private ERDOutlinePage outlinePage;

    /**
     * the <code>EditDomain</code>
     */
    private DefaultEditDomain editDomain;

    /**
     * the dirty state
     */
    private boolean isDirty;

    private boolean isLoaded;

    protected LoadingJob<EntityDiagram> diagramLoadingJob;
    private IPropertyChangeListener configPropertyListener;
    private PaletteRoot paletteRoot;

    private volatile String errorMessage;
    private ERDContentProvider contentProvider;
    private ERDDecorator decorator;
    private ZoomComboContributionItem zoomCombo;
    private NavigatorViewerAdapter navigatorViewerAdapter;
    private ERDHighlightingManager highlightingManager = new ERDHighlightingManager();

    private String exportMruFilename = null;
    private ERDConnectionRouterDescriptor routerStyle;
    private ERDNotationDescriptor notationStyle;

    /**
     * No-arg constructor
     */
    protected ERDEditorPart()
    {
    }

    public ERDContentProvider getContentProvider() {
        if (contentProvider == null) {
            contentProvider = createContentProvider();
        }
        return contentProvider;
    }

    public ERDDecorator getDecorator() {
        if (decorator == null) {
            decorator = createDecorator();
        }
        return decorator;
    }

    @NotNull
    public ERDHighlightingManager getHighlightingManager() {
        return highlightingManager;
    }

    /////////////////////////////////////////
    // INavigatorModelView implementation
    // We need it to support a set of standard commands like copy/paste/rename/etc

    @Override
    public DBNNode getRootNode() {
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof IDatabaseEditorInput) {
            return ((IDatabaseEditorInput) editorInput).getNavigatorNode();
        }
        return null;
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer() {
        if (navigatorViewerAdapter == null) {
            navigatorViewerAdapter = new NavigatorViewerAdapter();
        }
        return navigatorViewerAdapter;
    }

    protected ERDContentProvider createContentProvider() {
        return new ERDContentProviderDecorated();
    }

    protected ERDDecorator createDecorator() {
        return new ERDDecoratorDefault();
    }

    @Override
    protected ERDGraphicalViewer getGraphicalViewer() {
        return (ERDGraphicalViewer) super.getGraphicalViewer();
    }

    /**
     * Initializes the editor.
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        rootPart = new ScalableFreeformRootEditPart();
        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);

        super.init(site, input);

        // add  selection change listener
        //getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        configPropertyListener = new ConfigPropertyListener();
        ERDUIActivator.getDefault().getPreferenceStore().addPropertyChangeListener(configPropertyListener);
        if (routerStyle == null) {
            routerStyle = ERDConnectionRouterRegistry.getInstance().getActiveRouter();
        }
        if (notationStyle == null) {
            notationStyle = ERDNotationRegistry.getInstance().getActiveDescriptor();
        }
    }

    @Override
    public void createPartControl(Composite parent)
    {
        Composite contentContainer = parent;
        if (hasProgressControl()) {
            progressControl = new ProgressControl(parent, SWT.SHEET);
            progressControl.setShowDivider(true);
            contentContainer = progressControl.createContentContainer();
        } else {
            isLoaded = true;
        }

        super.createPartControl(contentContainer);

        if (hasProgressControl()) {
            progressControl.createProgressPanel();
        }
    }

    public DBECommandContext getCommandContext() {
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof IDatabaseEditorInput) {
            return ((IDatabaseEditorInput) editorInput).getCommandContext();
        }
        return null;
    }

    protected void updateToolbarActions() {
        if (progressControl != null) {
            progressControl.updateActions();

        }
    }

    /**
     * The <code>CommandStackListener</code> that listens for
     * <code>CommandStack </code> changes.
     */
    @Override
    public void commandStackChanged(EventObject event)
    {
        // Reevaluate properties
        ActionUtils.evaluatePropertyState(ERDEditorPropertyTester.NAMESPACE + "." + ERDEditorPropertyTester.PROP_CAN_UNDO);
        ActionUtils.evaluatePropertyState(ERDEditorPropertyTester.NAMESPACE + "." + ERDEditorPropertyTester.PROP_CAN_REDO);

        // Update actions
        setDirty(getCommandStack().isDirty());

        super.commandStackChanged(event);
    }

    @Override
    public void dispose()
    {
        ERDUIActivator.getDefault().getPreferenceStore().removePropertyChangeListener(configPropertyListener);

        if (diagramLoadingJob != null) {
            diagramLoadingJob.cancel();
            diagramLoadingJob = null;
        }
        // remove selection listener
        //getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        // dispose the ActionRegistry (will dispose all actions)
        getActionRegistry().dispose();
        // important: always call super implementation of dispose
        super.dispose();
    }

    /**
     * Adaptable implementation for Editor
     */
    @Override
    public Object getAdapter(Class adapter)
    {
        // we need to handle common .gef elements we created
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) {
            return getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return getCommandStack();
        } else if (adapter == EditDomain.class) {
            return getEditDomain();
        } else if (adapter == ActionRegistry.class) {
            return getActionRegistry();
        } else if (adapter == IPropertySheetPage.class) {
            return new PropertyPageStandard();
        } else if (adapter == IContentOutlinePage.class) {
            return getOverviewOutlinePage();
        } else if (adapter == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        } else if (IWorkbenchAdapter.class.equals(adapter)) {
            return new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o)
                {
                    return "ERD Editor";
                }
            };
        }
        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    @Override
    public abstract void doSave(IProgressMonitor monitor);

    /**
     * Save as not allowed
     */
    @Override
    public void doSaveAs()
    {
        saveDiagramAs();
    }

    /**
     * Save as not allowed
     */
    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
    }

    /**
     * Indicates if the editor has unsaved changes.
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty
     */
    @Override
    public boolean isDirty()
    {
        return !isReadOnly() && isDirty;
    }

    public abstract boolean isReadOnly();

    protected boolean hasProgressControl() {
        return true;
    }

    /**
     * Returns the <code>CommandStack</code> of this editor's
     * <code>EditDomain</code>.
     *
     * @return the <code>CommandStack</code>
     */
    @Override
    public CommandStack getCommandStack()
    {
        return getEditDomain().getCommandStack();
    }

    /**
     * Returns the schema model associated with the editor
     *
     * @return an instance of <code>Schema</code>
     */
    public EntityDiagram getDiagram()
    {
        return getDiagramPart().getDiagram();
    }

    public DiagramPart getDiagramPart()
    {
        return rootPart == null ? null : (DiagramPart) rootPart.getContents();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input)
    {
        super.setInput(input);
    }

    /**
     * Creates a PaletteViewerProvider that will be used to create palettes for
     * the view and the flyout.
     *
     * @return the palette provider
     */
    @Override
    protected PaletteViewerProvider createPaletteViewerProvider()
    {
        return new ERDPaletteViewerProvider(editDomain);
    }

    public GraphicalViewer getViewer()
    {
        return super.getGraphicalViewer();
    }

    /**
     * Creates a new <code>GraphicalViewer</code>, configures, registers and
     * initializes it.
     *
     * @param parent the parent composite
     */
    @Override
    protected void createGraphicalViewer(Composite parent)
    {
        GraphicalViewer viewer = createViewer(parent);

        // hook the viewer into the EditDomain
        setGraphicalViewer(viewer);

        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();

        // Set initial (empty) contents
        viewer.setContents(new EntityDiagram(null, "empty", getContentProvider(), getDecorator()));
        ERDEditorContextMenuProvider provider = createContextProvider();
        viewer.setContextMenu(provider);

        // Set context menu
        IWorkbenchPartSite site = getSite();
        if (site instanceof IEditorSite) {
            ((IEditorSite)site).registerContextMenu(ERDEditorPart.class.getName() + ".EditorContext", provider, viewer, false);
        } else {
            site.registerContextMenu(ERDEditorPart.class.getName() + ".EditorContext", provider, viewer);
        }
    }

    @NotNull
    protected ERDEditorContextMenuProvider createContextProvider() {
        return new ERDEditorContextMenuProvider(this, true);
    }

    private GraphicalViewer createViewer(Composite parent)
    {
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(getEditorSite());
        GraphicalViewer viewer = new ERDGraphicalViewer(this, validationMessageHandler);
        viewer.createControl(parent);

        // configure the viewer
        viewer.getControl().setBackground(ERDThemeSettings.instance.diagramBackground);
        viewer.setRootEditPart(rootPart);
        installKeyHandler(viewer);

        registerDropTargetListeners(viewer);

        // initialize the viewer with input
        viewer.setEditPartFactory(getDecorator().createPartFactory());

        return viewer;
    }

    protected void installKeyHandler(GraphicalViewer viewer) {
        viewer.setKeyHandler(new DBeaverNavigationKeyHandler(viewer));
    }

    protected void registerDropTargetListeners(GraphicalViewer viewer) {
        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));
        viewer.addDropTargetListener(new NodeDropTargetListener(viewer));
    }

    @Override
    protected void configureGraphicalViewer()
    {
        super.configureGraphicalViewer();
        this.getGraphicalViewer().getControl().setBackground(ERDThemeSettings.instance.diagramBackground);

        GraphicalViewer graphicalViewer = getGraphicalViewer();

/*
        MenuManager manager = new MenuManager(getClass().getName(), getClass().getName());
        manager.setRemoveAllWhenShown(true);
        getEditorSite().registerContextMenu(getClass().getName() + ".EditorContext", manager, graphicalViewer, true); //$NON-NLS-1$
*/

        DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();

        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, store.getBoolean(ERDUIConstants.PREF_GRID_ENABLED));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, store.getBoolean(ERDUIConstants.PREF_GRID_ENABLED));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(
            store.getInt(ERDUIConstants.PREF_GRID_WIDTH),
            store.getInt(ERDUIConstants.PREF_GRID_HEIGHT)));

        // initialize actions
        createActions();

        // Setup zoom manager
        ZoomManager zoomManager = rootPart.getZoomManager();

        List<String> zoomLevels = new ArrayList<>(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(zoomLevels);

        zoomManager.setZoomLevels(
            new double[]{.1, .1, .2, .3, .5, .6, .7, .8, .9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.5, 3, 4}
        );

        IAction zoomIn = new ZoomInAction(zoomManager);
        IAction zoomOut = new ZoomOutAction(zoomManager);
        addAction(zoomIn);
        addAction(zoomOut);
        if (editDomain.getPaletteViewer() != null) {
            addAction(new DiagramToggleHandAction(editDomain.getPaletteViewer()));
        }
        graphicalViewer.addSelectionChangedListener(event -> {
            String status;
            IStructuredSelection selection = (IStructuredSelection)event.getSelection();
            if (selection.isEmpty()) {
                status = "";
            } else if (selection.size() == 1) {
                status = CommonUtils.toString(selection.getFirstElement());
            } else {
                status = selection.size() + " objects";
            }
            if (progressControl != null) {
                progressControl.setInfo(status);
            }

            updateActions(editPartActionIDs);
        });
    }

    /**
     * Sets the dirty state of this editor.
     * <p/>
     * <p/>
     * An event will be fired immediately if the new state is different than the
     * current one.
     *
     * @param dirty the new dirty state to set
     */
    public void setDirty(boolean dirty)
    {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
     * Adds an action to this editor's <code>ActionRegistry</code>. (This is
     * a helper method.)
     *
     * @param action the action to add.
     */
    protected void addAction(IAction action)
    {
        getActionRegistry().registerAction(action);
        UIUtils.registerKeyBinding(getSite(), action);
    }

    /**
     * Updates the specified actions.
     *
     * @param actionIds the list of ids of actions to update
     */
    @Override
    protected void updateActions(List actionIds)
    {
        for (Object actionId : actionIds) {
            IAction action = getActionRegistry().getAction(actionId);
            if (null != action && action instanceof UpdateAction) {
                ((UpdateAction) action).update();
            }

        }
    }

    /**
     * Returns the overview for the outline view.
     *
     * @return the overview
     */
    protected ERDOutlinePage getOverviewOutlinePage()
    {
        if ((null == outlinePage || outlinePage.getControl().isDisposed()) && null != getGraphicalViewer()) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
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
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new PropertySheetPage();
            undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }

        return undoablePropertySheetPage;
    }

    /**
     * @return the preferences for the Palette Flyout
     */
    @Override
    protected ERDPalettePreferences getPalettePreferences()
    {
        return new ERDPalettePreferences();
    }

    /**
     * @return the PaletteRoot to be used with the PaletteViewer
     */
    @Override
    protected PaletteRoot getPaletteRoot()
    {
        if (paletteRoot == null) {
            paletteRoot = createPaletteRoot();
        }
        return paletteRoot;
    }

    public PaletteRoot createPaletteRoot()
    {
        // create root
        PaletteRoot paletteRoot = new PaletteRoot();
        paletteRoot.setLabel("Entity Diagram");

        getDecorator().fillPalette(paletteRoot, isReadOnly());

        return paletteRoot;

    }

    protected FlyoutPaletteComposite createPaletteComposite(Composite parent) {
        FlyoutPaletteComposite paletteComposite = new FlyoutPaletteComposite(
            parent,
            0,
            this.getSite().getPage(),
            this.getPaletteViewerProvider(),
            this.getPalettePreferences());
        paletteComposite.setBackground(ERDThemeSettings.instance.diagramBackground);
        return paletteComposite;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * The method designed to refresh diagram with metadata request  
     *
     * @param rearrange - re-arrange layout
     * @param reload - load diagram
     * @param refreshMetadata - reload metadata  
     */
    public void refreshDiagram(boolean rearrange, boolean reload, boolean refreshMetadata) {
        DiagramPart diagramPart = getDiagramPart();
        if (diagramPart == null) {
            return;
        }
        if (reload) {
            loadDiagram(refreshMetadata);
        }
        if (rearrange) {
            diagramPart.resetArrangement();
        }
    }

    /**
     * The method designed to refresh diagram without metadata request  
     *
     * @param rearrange - re-arrange layout
     * @param reload - load diagram 
     */
    public void refreshDiagram(boolean rearrange, boolean reload) {
        refreshDiagram(rearrange, reload, false);
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force)
    {
        refreshDiagram(force, true);
        return RefreshResult.REFRESHED;
    }

    public void saveDiagramAs()
    {
        List<ERDExportFormatRegistry.FormatDescriptor> allFormats = ERDExportFormatRegistry.getInstance().getFormats();
        String[] extensions = new String[allFormats.size()];
        String[] filterNames = new String[allFormats.size()];
        for (int i = 0; i < allFormats.size(); i++) {
            extensions[i] = "*." + allFormats.get(i).getExtension();
            filterNames[i] = allFormats.get(i).getLabel() + " (" + extensions[i] + ")";
        }
        final Shell shell = getSite().getShell();
        FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
        saveDialog.setFilterExtensions(extensions);
        saveDialog.setFilterNames(filterNames);

        String proposedFileName = exportMruFilename;
        if (CommonUtils.isEmpty(proposedFileName)) {
            proposedFileName = this.getTitle();
            if (!CommonUtils.isEmpty(proposedFileName)) {
                int extIndex = proposedFileName.lastIndexOf('.');
                if (extIndex != -1) {
                    proposedFileName = proposedFileName.substring(0, extIndex);
                }
            }
        }
        if (CommonUtils.isEmpty(proposedFileName)) {
            LinkedList<String> parts = new LinkedList<>();
            EntityDiagram diagram = this.getDiagram(); 
            DBSObject obj = diagram.getRootObjectContainer();
            if (obj == null && diagram.getEntities().size() > 0) {
                obj = diagram.getEntities().get(0).getObject();
            }
            while (obj != null && !(obj instanceof DBPDataSourceContainer)) {
                parts.addFirst(obj.getName());
                obj = obj.getParentObject();
            }
            
            if (parts.isEmpty()) {
                parts.add("unnammed");
            }
            proposedFileName = String.join(" - ", parts);
        }
        saveDialog.setFileName(proposedFileName);

        String filePath = DialogUtils.openFileDialog(saveDialog);
        if (filePath == null || filePath.trim().length() == 0) {
            return;
        }

        File outFile = new File(filePath);
        if (outFile.exists()) {
            if (!UIUtils.confirmAction(shell, "Overwrite file", "File '" + filePath + "' already exists.\nOverwrite?")) {
                return;
            }
        }

        exportMruFilename = outFile.getName();

        int divPos = filePath.lastIndexOf('.');
        if (divPos == -1) {
            DBWorkbench.getPlatformUI().showError("ERD export", "No file extension was specified");
            return;
        }
        String ext = filePath.substring(divPos + 1);
        ERDExportFormatRegistry.FormatDescriptor targetFormat = null;
        for (ERDExportFormatRegistry.FormatDescriptor format : allFormats) {
            if (format.getExtension().equals(ext)) {
                targetFormat = format;
                break;
            }
        }
        if (targetFormat == null) {
            DBWorkbench.getPlatformUI().showError("ERD export", "No export format correspond to file extension '" + ext + "'");
            return;
        }

        try {
            ERDExportFormatHandler formatHandler = targetFormat.getInstance();

            IFigure figure = rootPart.getLayer(ScalableFreeformRootEditPart.PRINTABLE_LAYERS);

            formatHandler.exportDiagram(getDiagram(), figure, getDiagramPart(), outFile);

            final int openFileDecision = ConfirmationDialog.confirmAction(
                getGraphicalControl().getShell(),
                ERDUIConstants.CONFIRM_OPEN_EXPORTED_FILE,
                ConfirmationDialog.QUESTION);

            if (openFileDecision == IDialogConstants.YES_ID) {
                ShellUtils.launchProgram(outFile.getAbsolutePath());
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("ERD export failed", null, e);
        }
    }

    public void fillAttributeVisibilityMenu(IMenuManager menu)
    {
        MenuManager asMenu = new MenuManager(ERDUIMessages.menu_view_style);
        for (ERDViewStyle style : ERDViewStyle.values()) {
            if (decorator.supportsAttributeStyle(style)) {
                asMenu.add(new ChangeAttributePresentationAction(style));
            }
        }
        if (!asMenu.isEmpty()) {
            menu.add(asMenu);
        }

        if (getDiagram().getDecorator().supportsAttributeVisibility()) {
            MenuManager avMenu = new MenuManager(ERDUIMessages.menu_attribute_visibility);
            avMenu.add(new EmptyAction(ERDUIMessages.menu_attribute_visibility_default));
            avMenu.add(new ChangeAttributeVisibilityAction(true, ERDAttributeVisibility.ALL));
            avMenu.add(new ChangeAttributeVisibilityAction(true, ERDAttributeVisibility.KEYS));
            avMenu.add(new ChangeAttributeVisibilityAction(true, ERDAttributeVisibility.PRIMARY));
            avMenu.add(new ChangeAttributeVisibilityAction(true, ERDAttributeVisibility.NONE));

            ISelection selection = getGraphicalViewer().getSelection();
            if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                int totalEntities = 0;
                for (Object item : ((IStructuredSelection) selection).toArray()) {
                    if (item instanceof EntityPart) {
                        totalEntities++;
                    }
                }

                if (totalEntities > 0) {
                    avMenu.add(new Separator());
                    String avaTitle = ERDUIMessages.menu_attribute_visibility_entity;
                    if (((IStructuredSelection) selection).size() == 1) {
                        avaTitle += " (" + ((IStructuredSelection) selection).getFirstElement() + ")";
                    } else {
                        avaTitle += " (" + totalEntities + ")";
                    }
                    avMenu.add(new EmptyAction(avaTitle));
                    avMenu.add(new ChangeAttributeVisibilityAction(false, ERDAttributeVisibility.ALL));
                    avMenu.add(new ChangeAttributeVisibilityAction(false, ERDAttributeVisibility.KEYS));
                    avMenu.add(new ChangeAttributeVisibilityAction(false, ERDAttributeVisibility.PRIMARY));
                    avMenu.add(new ChangeAttributeVisibilityAction(false, ERDAttributeVisibility.NONE));
                }
            }
            menu.add(avMenu);
        }
    }

    /**
     * Fill ERD notations popup menu
     *
     * @param menu - root node
     */
    public void fillNotationsMenu(IMenuManager menu) {
        MenuManager ntMenu = new MenuManager(ERDUIMessages.menu_notation_style);
        for (ERDNotationDescriptor ntType : ERDNotationRegistry.getInstance().getNotations()) {
            ntMenu.add(new ChangeERDNotationStyleAction(ntType));
        }
        menu.add(ntMenu);
    }

    /**
     * Fill ERD routers popup menu
     *
     * @param menu - root node
     */
    public void fillRoutersMenu(IMenuManager menu) {
        MenuManager ntMenu = new MenuManager(ERDUIMessages.menu_router_style);
        for (ERDConnectionRouterDescriptor ntType : ERDConnectionRouterRegistry.getInstance().getDescriptors()) {
            ntMenu.add(new ChangeERDRouterStyleAction(ntType));
        }
        menu.add(ntMenu);
    }

    public void fillPartContextMenu(IMenuManager menu, IStructuredSelection selection) {
        if (selection.isEmpty()) {
            return;
        }
        if (selection.getFirstElement() instanceof IMenuListener) {
            ((IMenuListener) selection.getFirstElement()).menuAboutToShow(menu);
        }
        menu.add(new ChangeZOrderAction(this, selection, true));
        menu.add(new ChangeZOrderAction(this, selection, false));
        menu.add(new SetPartColorAction(this, selection));
        ResetPartColorAction resetPartColorAction = new ResetPartColorAction(this, selection);
        if (resetPartColorAction.isEnabled()) {
            menu.add(resetPartColorAction);
        }
        SetPartSettingsAction settingsAction = new SetPartSettingsAction(this, selection);
        if (settingsAction.isEnabled()) {
            menu.add(settingsAction);
        }

/*
        Set<IAction> actionSet = new HashSet<>();
        for (Object actionId : getSelectionActions()) {
            IAction action = getActionRegistry().getAction(actionId);
            if (!actionSet.contains(action)) {
                menu.add(action);
                actionSet.add(action);
            }
        }
*/
    }

    public void printDiagram()
    {
        GraphicalViewer viewer = getGraphicalViewer();

        PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
        PrinterData data = dialog.open();

        if (data != null) {
            IFigure rootFigure = rootPart.getLayer(ScalableFreeformRootEditPart.PRINTABLE_LAYERS);
            //EntityDiagramFigure diagramFigure = findFigure(rootFigure, EntityDiagramFigure.class);
            if (rootFigure != null) {
                PrintFigureOperation printOp = new PrintFigureOperation(new Printer(data), rootFigure);

                // Set print preferences
                DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
                printOp.setPrintMode(store.getInt(ERDUIConstants.PREF_PRINT_PAGE_MODE));
                printOp.setPrintMargin(new Insets(
                    store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_TOP),
                    store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_LEFT),
                    store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_BOTTOM),
                    store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_RIGHT)
                ));
                // Run print
                printOp.run("Print ER diagram");
            }
        }
        //new PrintAction(this).run();
    }

    @Override
    public boolean isSearchPossible()
    {
        return true;
    }

    @Override
    public boolean isSearchEnabled()
    {
        return progressControl != null && progressControl.isSearchEnabled();
    }

    @Override
    public boolean performSearch(SearchType searchType)
    {
        return progressControl != null && progressControl.performSearch(searchType);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    protected void fillDefaultEditorContributions(IContributionManager toolBarManager) {
        ZoomManager zoomManager = rootPart.getZoomManager();

        String[] zoomStrings = new String[]{
            ZoomManager.FIT_ALL,
            ZoomManager.FIT_HEIGHT,
            ZoomManager.FIT_WIDTH
        };
        // Init zoom combo with dummy part service
        // to prevent zoom disable on part change - as it is standalone zoom control, not global one
        zoomCombo = new ZoomComboContributionItem(
            new IPartService() {
                @Override
                public void addPartListener(IPartListener listener)
                {
                }

                @Override
                public void addPartListener(IPartListener2 listener)
                {
                }

                @Override
                public IWorkbenchPart getActivePart()
                {
                    return ERDEditorPart.this;
                }

                @Override
                public IWorkbenchPartReference getActivePartReference()
                {
                    return null;
                }

                @Override
                public void removePartListener(IPartListener listener)
                {
                }

                @Override
                public void removePartListener(IPartListener2 listener)
                {
                }
            },
            zoomStrings);
        zoomCombo.setZoomManager(zoomManager);

        toolBarManager.add(zoomCombo);

        //toolBarManager.add(new UndoAction(ERDEditorPart.this));
        //toolBarManager.add(new RedoAction(ERDEditorPart.this));
        //toolBarManager.add(new PrintAction(ERDEditorPart.this));

        ZoomInAction zoomInAction = new ZoomInAction(zoomManager);
        zoomInAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_IN));
        ZoomOutAction zoomOutAction = new ZoomOutAction(zoomManager);
        zoomOutAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_OUT));
        toolBarManager.add(zoomInAction);
        toolBarManager.add(zoomOutAction);

        toolBarManager.add(new Separator());
        //toolBarManager.add(createAttributeVisibilityMenu());
        toolBarManager.add(new DiagramLayoutAction(ERDEditorPart.this));
        toolBarManager.add(new DiagramToggleGridAction());
        if (editDomain.getPaletteViewer() != null) {
            toolBarManager.add(new DiagramToggleHandAction(editDomain.getPaletteViewer()));
        }
        toolBarManager.add(new Separator());
        toolBarManager.add(new ToggleViewAction(IPageLayout.ID_PROP_SHEET));
        toolBarManager.add(new ToggleViewAction(IPageLayout.ID_OUTLINE));
        //toolBarManager.add(new DiagramRefreshAction(ERDEditorPart.this));
        toolBarManager.add(new Separator());
        {
            toolBarManager.add(ActionUtils.makeCommandContribution(
                getSite(),
                IWorkbenchCommandConstants.FILE_PRINT,
                ERDUIMessages.erd_editor_control_action_print_diagram,
                UIIcon.PRINT));

            toolBarManager.add(ActionUtils.makeCommandContribution(getSite(), ERDUIConstants.CMD_SAVE_AS));
        }
        fillConfigurationContribution(toolBarManager);
    }
    
    protected void fillConfigurationContribution(IContributionManager toolBarManager) {
        toolBarManager.add(new Separator("configuration"));
        Action configAction = new Action(ERDUIMessages.erd_editor_control_action_configuration) {
            @Override
            public void run() {
                UIUtils.showPreferencesFor(
                    getSite().getShell(),
                    ERDEditorPart.this,
                    ERDPreferencePage.PAGE_ID);
                getDiagram().setAttributeStyles(ERDViewStyle.getDefaultStyles(ERDUIActivator.getDefault().getPreferences()));
            }
        };
        configAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
        toolBarManager.add(configAction);
    }

    protected abstract void loadDiagram(boolean refreshMetadata);

    @Nullable
    public abstract DBPProject getDiagramProject();

    @Override
    public boolean isActiveTask() {
        return diagramLoadingJob != null && diagramLoadingJob.getState() == Job.RUNNING;
    }

    @Override
    public boolean isModelEditEnabled() {
        return getDiagram().isEditEnabled();
    }

    @Override
    public boolean containsModelObject(DBSObject object) {
        return object instanceof DBSEntity && getDiagram().getEntity((DBSEntity) object) != null;
    }

    private class ChangeAttributePresentationAction extends Action {
        private final ERDViewStyle style;
        public ChangeAttributePresentationAction(ERDViewStyle style) {
            super(style.getActionTitle(), AS_CHECK_BOX);
            this.style = style;
        }
        @Override
        public boolean isChecked()
        {
            return ArrayUtils.contains(
                ERDViewStyle.getDefaultStyles(ERDUIActivator.getDefault().getPreferences()),
                style);
        }

        @Override
        public void run()
        {
            getDiagram().setAttributeStyle(style, !isChecked());
            refreshEntityAndAttributes();
            refreshDiagram(true, false);
        }
    }
    
    private class ChangeERDNotationStyleAction extends Action {
        private final ERDNotationDescriptor notation;

        public ChangeERDNotationStyleAction(@NotNull ERDNotationDescriptor notation) {
            super(notation.getName(), AS_CHECK_BOX);
            this.notation = notation;
        }

        @Override
        public boolean isChecked() {
            if (notation == null || getDiagramNotation() == null) {
                return false;
            }
            return notation.getId().equals(getDiagramNotation().getId());
        }

        @Override
        public void run() {
            setDiagramNotation(notation);
            refreshDiagram(false, true);
        }
    }

    private class ChangeERDRouterStyleAction extends Action {
        private final ERDConnectionRouterDescriptor router;

        public ChangeERDRouterStyleAction(@NotNull ERDConnectionRouterDescriptor router) {
            super(router.getName(), AS_CHECK_BOX);
            this.router = router;
        }

        @Override
        public boolean isChecked() {
            if (router == null || getDiagramRouter() == null) {
                return false;
            }
            return router.getId().equals(getDiagramRouter().getId());
        }

        @Override
        public void run() {
            setDiagramRouter(router);
            refreshDiagram(false, true);
        }
    }

    private class ChangeAttributeVisibilityAction extends Action {
        private final boolean defStyle;
        private final ERDAttributeVisibility visibility;

        private ChangeAttributeVisibilityAction(boolean defStyle, ERDAttributeVisibility visibility)
        {
            super(visibility.getTitle() + "", IAction.AS_CHECK_BOX);
            this.defStyle = defStyle;
            this.visibility = visibility;
        }

        @Override
        public boolean isChecked()
        {
            if (defStyle) {
                return visibility == getDiagram().getAttributeVisibility();
            } else {
                for (Object object : ((IStructuredSelection)getGraphicalViewer().getSelection()).toArray()) {
                    if (object instanceof EntityPart) {
                        ERDAttributeVisibility entityAV = ((EntityPart) object).getEntity().getAttributeVisibility();
                        if (entityAV == null) {
                            return visibility == getDiagram().getAttributeVisibility();
                        } else if (entityAV == visibility) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        @Override
        public void runWithEvent(Event event) {
            super.runWithEvent(event);
        }

        @Override
        public void run()
        {
            EntityDiagram diagram = getDiagram();
            if (defStyle) {
                diagram.setAttributeVisibility(visibility);
                for (ERDEntity entity : diagram.getEntities()) {
                    entity.reloadAttributes(diagram);
                }
                refreshEntityAndAttributes();
            } else {
                for (Object object : ((IStructuredSelection)getGraphicalViewer().getSelection()).toArray()) {
                    if (object instanceof EntityPart) {
                        ((EntityPart) object).getEntity().setAttributeVisibility(visibility);
                        UIUtils.asyncExec(() -> {
                            ((EntityPart) object).getEntity().reloadAttributes(diagram);
                            ((EntityPart) object).refresh();
                        });

                    }
                }
            }
            refreshDiagram(true, false);
        }
    }

    private class ConfigPropertyListener implements IPropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event)
        {
            GraphicalViewer graphicalViewer = getGraphicalViewer();
            if (graphicalViewer == null) {
                return;
            }
            if (ERDUIConstants.PREF_GRID_ENABLED.equals(event.getProperty())) {
                Boolean enabled = Boolean.valueOf(event.getNewValue().toString());
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, enabled);
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, enabled);
            } else if (ERDUIConstants.PREF_GRID_WIDTH.equals(event.getProperty()) || ERDUIConstants.PREF_GRID_HEIGHT.equals(event.getProperty())) {
                final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(
                    store.getInt(ERDUIConstants.PREF_GRID_WIDTH),
                    store.getInt(ERDUIConstants.PREF_GRID_HEIGHT)));
            } else if (ERDConstants.PREF_ATTR_VISIBILITY.equals(event.getProperty())) {
                EntityDiagram diagram = getDiagram();
                ERDAttributeVisibility attrVisibility = CommonUtils.valueOf(ERDAttributeVisibility.class, CommonUtils.toString(event.getNewValue()));
                diagram.setAttributeVisibility(attrVisibility);
                refreshEntityAndAttributes();
            } else if (ERDConstants.PREF_ATTR_STYLES.equals(event.getProperty())) {
                refreshEntityAndAttributes();
            } else if (ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS.equals(event.getProperty()) ||
                ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS.equals(event.getProperty())) {
                doSave(new NullProgressMonitor());
                refreshDiagram(true, false);
            } else if (ERDUIConstants.PREF_NOTATION_TYPE.equals(event.getProperty())) {
                ERDNotationDescriptor defaultNotation = ERDNotationRegistry.getInstance().getActiveDescriptor();
                setDiagramNotation(defaultNotation);
                doSave(new NullProgressMonitor());
                refreshDiagram(true, false);
            } else if (ERDUIConstants.PREF_ROUTING_TYPE.equals(event.getProperty())) {
                ERDConnectionRouterDescriptor defaultRouter = ERDConnectionRouterRegistry.getInstance().getActiveRouter();
                setDiagramRouter(defaultRouter);
                doSave(new NullProgressMonitor());
                refreshDiagram(true, false);
            }
        }
    }

    private void refreshEntityAndAttributes() {
        getDiagram().getEntities().forEach(entity -> {
            entity.reloadAttributes(getDiagram());
        });
        getGraphicalViewer().getContents().getChildren().forEach(editPart -> {
            if (editPart instanceof EntityPart) {
                ((EntityPart) editPart).refresh();
            }
        });
    }

    public class ProgressControl extends ProgressPageControl {

        private final Searcher searcher;

        private ProgressControl(Composite parent, int style)
        {
            super(parent, style);
            searcher = new Searcher();
        }

        @Override
        protected boolean cancelProgress()
        {
            if (diagramLoadingJob != null) {
                diagramLoadingJob.cancel();
                return true;
            }
            return false;
        }

        public ProgressVisualizer<EntityDiagram> createLoadVisualizer()
        {
            Control graphicalControl = getGraphicalControl();
            if (graphicalControl != null) {
                graphicalControl.setBackground(ERDThemeSettings.instance.diagramBackground);
            }
            return new LoadVisualizer();
        }

        @Override
        public void fillCustomActions(IContributionManager toolBarManager) {
            fillDefaultEditorContributions(toolBarManager);
        }

        @Override
        protected void populateCustomActions(ContributionManager contributionManager) {
            ToolBarManager extToolBar = new ToolBarManager();
            // Add dynamic toolbar contributions
            final IMenuService menuService = getSite().getService(IMenuService.class);
            if (menuService != null) {
                menuService.populateContributionManager(extToolBar , "toolbar:ERDEditorToolbar");
            }
            if (!extToolBar.isEmpty()) {
                boolean hasSave = contributionManager.find("save") != null;
                for (IContributionItem item : extToolBar.getItems()) {
                    if (hasSave) {
                        contributionManager.insertAfter("save", item);
                    } else {
                        contributionManager.insertAfter("configuration", item);
                    }
                }
                contributionManager.update(true);
            }
        }

        @Override
        protected ISearchExecutor getSearchRunner()
        {
            return searcher;
        }

        public class LoadVisualizer extends ProgressVisualizer<EntityDiagram> {
            @Override
            public void visualizeLoading()
            {
                super.visualizeLoading();
            }

            @Override
            public void completeLoading(EntityDiagram entityDiagram)
            {
                super.completeLoading(entityDiagram);
                if (entityDiagram != null) {
                    List<String> errorMessages = entityDiagram.getErrorMessages();
                    if (!errorMessages.isEmpty()) {
                        List<Status> messageStatuses = new ArrayList<>(errorMessages.size());
                        for (String error : errorMessages) {
                            messageStatuses.add(new Status(Status.ERROR, ERDUIActivator.PLUGIN_ID, error));
                        }
                        MultiStatus status = new MultiStatus(ERDUIActivator.PLUGIN_ID, 0, messageStatuses.toArray(new IStatus[0]), null, null);

                        DBWorkbench.getPlatformUI().showError(
                                "Diagram loading errors",
                            "Error(s) occurred during diagram loading. If these errors are recoverable then fix errors and then refresh/reopen diagram",
                            status);
                    }
                    setInfo(entityDiagram.getEntityCount() + " objects");
                } else {
                    setInfo("Empty diagram due to error (see error log)");
                }
            }
        }

    }

    /**
     * The method process entity relation diagram for element visualization
     * 
     * @param monitor - DBRProgressMonitor
     * @param entityDiagram - EntityDiagram
     * @return - EntityDiagram
     */
    public EntityDiagram visuallize(DBRProgressMonitor monitor, EntityDiagram entityDiagram) {
        if (monitor.isCanceled()) {
            return entityDiagram;
        }
        monitor.beginTask(ERDUIMessages.erd_job_visuallize_content, 4);
        Control graphicalControl = getGraphicalControl();
        if (graphicalControl == null) {
            return entityDiagram;
        }
        UIUtils.syncExec(() -> graphicalControl.setBackground(ERDThemeSettings.instance.diagramBackground));
        isLoaded = true;
        Control control = getGraphicalViewer().getControl();
        if (control == null || control.isDisposed()) {
            return entityDiagram;
        }
        if (monitor.isCanceled()) {
            return entityDiagram;
        } else {
            monitor.worked(1);
        }
        UIUtils.syncExec(() -> getCommandStack().flush());
        if (entityDiagram != null) {
            if (entityDiagram.isDirty()) {
                // Associated connections were changed during the loading process
                getCommandStack().execute(new MarkDirtyCommand());
            }
            if (monitor.isCanceled()) {
                return entityDiagram;
            } else {
                monitor.worked(1);
            }
            EditPart oldContents = getGraphicalViewer().getContents();
            if ((oldContents instanceof DiagramPart diagramPart) &&
                restoreVisualSettings(diagramPart, entityDiagram)) {
                entityDiagram.setLayoutManualAllowed(true);
                entityDiagram.setLayoutManualDesired(true);
            }
            if (monitor.isCanceled()) {
                return entityDiagram;
            } else {
                monitor.worked(1);
            }
            UIUtils.syncExec(() -> getGraphicalViewer().setContents(entityDiagram));
        }
        if (monitor.isCanceled()) {
            return entityDiagram;
        } else {
            monitor.worked(1);
        }
        if (zoomCombo != null) {
            zoomCombo.setZoomManager(rootPart.getZoomManager());
        }
        return entityDiagram;
    }

    protected boolean restoreVisualSettings(DiagramPart oldDiagram, EntityDiagram newDiagram) {
        boolean hasChanges = false;
        // Collect visual settings from old diagram and apply them to the new one
        for (ERDEntity newEntity : newDiagram.getEntities()) {
            NodePart oldEntity = oldDiagram.getChildByObject(newEntity.getObject());
            if (oldEntity instanceof EntityPart) {
                EntityDiagram.NodeVisualInfo vi = new EntityDiagram.NodeVisualInfo((EntityPart) oldEntity);
                newDiagram.addVisualInfo(newEntity.getObject(), vi);
                hasChanges = true;
            }
        }

        for (ERDNote newNote : newDiagram.getNotes()) {
            NodePart oldNotePart = oldDiagram.getChildByObject(newNote.getObject());
            if (oldNotePart instanceof NotePart) {
                EntityDiagram.NodeVisualInfo vi = new EntityDiagram.NodeVisualInfo((NotePart) oldNotePart);
                vi.initBounds = oldNotePart.getBounds();
                newDiagram.addVisualInfo(newNote, vi);
                hasChanges = true;
            }
        }
        boolean hasLayout = false;
        for (ERDEntity entity : newDiagram.getEntities()) {
            NodeVisualInfo visualInfo = newDiagram.getVisualInfo(entity.getObject());
            if (visualInfo != null && visualInfo.initBounds != null && visualInfo.initBounds.x != 0 && visualInfo.initBounds.y != 0) {
                hasLayout = true;
                break;
            }
        }
        return hasChanges && hasLayout;
    }

    private class Searcher implements ISearchExecutor {
        @Nullable
        private String searchString = null;
        @Nullable
        private Pattern curSearchPattern;
        private boolean resultsFound;
        private Boolean isPrevStepWasFwd;
        @Nullable
        private List<Object> results = null;
        @Nullable
        private ListIterator<Object> resultsIterator = null;
        @Nullable
        private Object currentItem = null;
        @Nullable
        private List<ERDHighlightingHandle> highlightings = new LinkedList<>();
        
        @Override
        public boolean performSearch(@NotNull String searchString, int options) {
            if (this.results != null && this.searchString != null && this.searchString.equals(searchString)) {
                return findNextResult(options == SEARCH_NEXT);
            } else {
                this.cancelSearch();
                isPrevStepWasFwd = null;
                results = new ArrayList<>();
                this.searchString = searchString;
                String likePattern = SQLUtils.makeLikePattern(searchString);
                if (likePattern.isEmpty() || (curSearchPattern != null && likePattern.equals(curSearchPattern.pattern()))) {
                    return resultsFound;
                }

                try {
                    curSearchPattern = Pattern.compile(likePattern, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    searcherLog.warn("Unable to perform search in ERD editor due to an inability to compile search pattern", e);
                    if (progressControl != null) {
                        progressControl.setInfo(e.getMessage());
                    }
                    return false;
                }

                resultsFound = false;
                ERDGraphicalViewer graphicalViewer = getGraphicalViewer();
                graphicalViewer.deselectAll();
                Set<DBPNamedObject> nodes = new HashSet<>();
                for (Object node : getDiagramPart().getChildren()) {
                    if (node instanceof DBPNamedObject && node instanceof EditPart) {
                        nodes.add((DBPNamedObject) node);
                    }
                    if (node instanceof EntityPart) {
                        List<?> children = ((EntityPart) node).getChildren();
                        if (!CommonUtils.isEmpty(children)) {
                            for (Object child: children) {
                                if (child instanceof DBPNamedObject && child instanceof EditPart) {
                                    nodes.add((DBPNamedObject) child);
                                }
                            }
                        }
                    }
                }
                if (!CommonUtils.isEmpty(nodes)) {
                    Color color = ERDThemeSettings.instance.searchHighlightColor;
                    DBPNamedObject focusedNode = null;
                    for (DBPNamedObject erdNode : nodes) {
                        if (matchesSearch(erdNode)) {
                            if (!resultsFound) {
                                focusedNode = erdNode; // let's set focus to the first found node after search complete
                            }
                            resultsFound = true;
                            results.add(erdNode);
                            if (erdNode instanceof GraphicalEditPart) {
                                highlightings.add(highlightingManager.highlight(((GraphicalEditPart) erdNode).getFigure(), color));
                            }
                        }
                    }
                    if (resultsFound && focusedNode != null) {
                        graphicalViewer.reveal((EditPart) focusedNode);
                    }
                }
                resultsIterator = results.listIterator();
                return resultsFound;
            }
        }
        
        private void jumpToNext(boolean isFindNext) {
            if (resultsIterator == null || isFindNext ? !resultsIterator.hasNext() : !resultsIterator.hasPrevious()) {
                resultsIterator = results.listIterator(isFindNext ? 0 : results.size());
            }
            currentItem = isFindNext ? resultsIterator.next() : resultsIterator.previous();
        }

        private boolean findNextResult(boolean isFindNext) {
            if (resultsFound && results != null) {
                if (isPrevStepWasFwd != null && isPrevStepWasFwd.booleanValue() != isFindNext) { 
                    // direction change gets current item again as if it's a new loop initialization 
                    jumpToNext(isFindNext); 
                }
                isPrevStepWasFwd = isFindNext;
                jumpToNext(isFindNext);

                ERDGraphicalViewer graphicalViewer = getGraphicalViewer();
                graphicalViewer.deselectAll();
                graphicalViewer.select((EditPart) currentItem);
                graphicalViewer.reveal((EditPart) currentItem);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void cancelSearch() {
            if (curSearchPattern != null) {
                curSearchPattern = null;
                if (resultsFound) {
                    resultsFound = false;
                    results = null;
                    currentItem = null;
                    resultsIterator = null;
                    highlightings.forEach(ERDHighlightingHandle::release);
                    highlightings.clear();
                    getGraphicalViewer().deselectAll();
                }
            }
        }

        private boolean matchesSearch(@Nullable DBPNamedObject element) {
            if (curSearchPattern == null || element == null) {
                return false;
            }
            return curSearchPattern.matcher(element.getName()).find();
        }
    }

    protected abstract class DiagramLoaderVisualizer extends ProgressLoaderVisualizer<EntityDiagram> {
        protected DiagramLoaderVisualizer(ILoadService<EntityDiagram> loadingService, Composite control) {
            super(loadingService, control);
        }

        @Override
        public void visualizeLoading() {
            super.visualizeLoading();
        }

        @Override
        public void completeLoading(EntityDiagram entityDiagram) {
            super.completeLoading(entityDiagram);
            super.visualizeLoading();
            if (entityDiagram != null && !entityDiagram.getEntities().isEmpty()) {
                setErrorMessage(null);
            } else {
                List<String> errorMessages = entityDiagram.getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    List<Status> messageStatuses = new ArrayList<>(errorMessages.size());
                    for (String error : errorMessages) {
                        messageStatuses.add(new Status(Status.ERROR, ERDUIActivator.PLUGIN_ID, error));
                    }
                    MultiStatus status = new MultiStatus(ERDUIActivator.PLUGIN_ID, 0, messageStatuses.toArray(new IStatus[0]), null, null);
                    DBWorkbench.getPlatformUI().showError(
                        "VQB Diagram loading errors",
                        "Error(s) occurred during diagram loading."
                        + " If these errors are recoverable then fix errors and then refresh/reopen diagram",
                        status);
                }
            }
        }

        protected abstract void finishLoading();
    }

    private class NavigatorViewerAdapter extends Viewer {
        @Override
        public Control getControl() {
            return getGraphicalViewer() == null ? null : getGraphicalControl();
        }

        @Override
        public Object getInput() {
            return getRootNode();
        }

        @Override
        public ISelection getSelection() {
            return getViewer().getSelection();
        }

        @Override
        public void refresh() {
            refreshDiagram(false, false);
        }

        @Override
        public void setInput(Object input) {

        }

        @Override
        public void setSelection(ISelection selection, boolean reveal) {

        }
    }

    /**
     * A special command that marks the editor dirty without the possibility to undo it.
     */
    private static class MarkDirtyCommand extends Command {
        @Override
        public boolean canExecute() {
            return true;
        }

        @Override
        public boolean canRedo() {
            return false;
        }

        @Override
        public boolean canUndo() {
            return false;
        }
    }

    public ERDNotationDescriptor getDiagramNotation() {
        return notationStyle;
    }

    public void setDiagramNotation(ERDNotationDescriptor notation) {
        this.notationStyle = notation;
    }

    public ERDConnectionRouterDescriptor getDiagramRouter() {
        return routerStyle;
    }

    public void setDiagramRouter(ERDConnectionRouterDescriptor router) {
        this.routerStyle = router;
    }

    public ProgressControl getProgressControl() {
        if (progressControl == null || progressControl.isDisposed()) {
            progressControl = new ProgressControl((Composite) super.getGraphicalControl(), SWT.SHEET);
            progressControl.setShowDivider(true);
        }
        return this.progressControl;
    }
}
