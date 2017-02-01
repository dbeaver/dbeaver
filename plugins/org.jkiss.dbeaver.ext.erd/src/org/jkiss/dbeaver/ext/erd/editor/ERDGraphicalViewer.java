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
/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * GraphicalViewer which also knows about ValidationMessageHandler to output
 * error messages to
 * @author Serge Rider
 */
public class ERDGraphicalViewer extends ScrollingGraphicalViewer implements IPropertyChangeListener, DBPEventListener {
    private static final Log log = Log.getLog(ERDGraphicalViewer.class);

    private ERDEditorPart editor;
	private ValidationMessageHandler messageHandler;
    private IThemeManager themeManager;
    private boolean loadContents = false;

    private static class DataSourceInfo {
        int tableCount = 0;
    }

    private final Map<DBPDataSourceContainer, DataSourceInfo> usedDataSources = new IdentityHashMap<>();

	/**
	 * ValidationMessageHandler to receive messages
	 * @param messageHandler message handler 
	 */
	public ERDGraphicalViewer(ERDEditorPart editor, ValidationMessageHandler messageHandler)
	{
		super();
        this.editor = editor;
		this.messageHandler = messageHandler;

        themeManager = editor.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(this);
	}

    public ERDEditorPart getEditor()
    {
        return editor;
    }

    @Override
    public void setControl(Control control)
    {
        super.setControl(control);

        if (control != null) {
            ERDEditorAdapter.mapControl(control, editor);
            UIUtils.addFocusTracker(editor.getSite(), ERDConstants.ERD_CONTROL_ID, control);
            applyThemeSettings();
        }
    }

    @Override
    protected void handleDispose(DisposeEvent e) {
        if (themeManager != null) {
            themeManager.removePropertyChangeListener(this);
        }
        if (getControl() != null) {
            ERDEditorAdapter.unmapControl(getControl());
            UIUtils.removeFocusTracker(editor.getSite(), getControl());
        }
        super.handleDispose(e);
    }

    /**
	 * @return Returns the messageLabel.
	 */
	public ValidationMessageHandler getValidationHandler()
	{
		return messageHandler;
	}

	/**
	 * This method is invoked when this viewer's control loses focus. It removes
	 * focus from the {@link AbstractEditPartViewer#focusPart focusPart}, if
	 * there is one.
	 * 
	 * @param fe
	 *            the focusEvent received by this viewer's control
	 */
	@Override
    protected void handleFocusLost(FocusEvent fe)
	{
		//give the superclass a chance to handle this first
		super.handleFocusLost(fe);
		//call reset on the MessageHandler itself
		messageHandler.reset();
	}

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)
            || event.getProperty().equals("org.jkiss.dbeaver.erd.diagram.font"))
        {
            applyThemeSettings();
        }
    }

    private void applyThemeSettings()
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Font erdFont = currentTheme.getFontRegistry().get("org.jkiss.dbeaver.erd.diagram.font");
        if (erdFont != null) {
            this.getControl().setFont(erdFont);
        }
        editor.refreshDiagram(true);
/*
        DiagramPart diagramPart = editor.getDiagramPart();
        if (diagramPart != null) {
            diagramPart.resetFonts();
            diagramPart.refresh();
        }
*/
    }

    @Override
    public void setContents(EditPart editpart)
    {
        loadContents = true;
        try {
            super.setContents(editpart);
            // Reset palette contents
            if (editpart instanceof DiagramPart) {
                List<DBSEntity> tables = new ArrayList<>();
                for (Object child : editpart.getChildren()) {
                    if (child instanceof EntityPart) {
                        tables.add(((EntityPart) child).getTable().getObject());
                    }
                }
                Collections.sort(tables, DBUtils.<DBSEntity>nameComparator());
                Map<PaletteDrawer, List<ToolEntryTable>> toolMap = new LinkedHashMap<>();
                for (DBSEntity table : tables) {
                    DBPDataSourceContainer container = table.getDataSource().getContainer();
                    PaletteDrawer drawer = getContainerPaletteDrawer(container);
                    if (drawer != null) {
                        List<ToolEntryTable> tools = toolMap.get(drawer);
                        if (tools == null) {
                            tools = new ArrayList<>(tables.size());
                            toolMap.put(drawer, tools);
                        }
                        tools.add(new ToolEntryTable(table));
                    }
                }
                for (Map.Entry<PaletteDrawer, List<ToolEntryTable>> entry : toolMap.entrySet()) {
                    entry.getKey().setChildren(entry.getValue());
                }
                //editor.getPaletteContents().setChildren(tools);
            }
        }
        finally {
            loadContents = false;
        }
    }

    public void handleTableActivate(DBSEntity table)
    {
        if (table.getDataSource() != null) {
            DBPDataSourceContainer container = table.getDataSource().getContainer();
            if (container != null) {
                synchronized (usedDataSources) {
                    DataSourceInfo dataSourceInfo = usedDataSources.get(container);
                    if (dataSourceInfo == null) {
                        dataSourceInfo = new DataSourceInfo();
                        usedDataSources.put(container, dataSourceInfo);
                        acquireContainer(container);
                    }
                    dataSourceInfo.tableCount++;
                }
            }
        }

        if (!loadContents) {
            final PaletteContainer drawer = getContainerPaletteDrawer(table.getDataSource().getContainer());
            if (drawer != null) {
                // Add entry (with right order)
                List children = drawer.getChildren();
                int index = 0;
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    Object child = children.get(i);
                    if (child instanceof ToolEntryTable) {
                        if (((ToolEntryTable) child).table.getName().compareTo(table.getName()) > 0) {
                            index = i;
                            break;
                        }
                    }
                }
                drawer.add(index, new ToolEntryTable(table));
            }
        }
    }

    public void handleTableDeactivate(DBSEntity table)
    {
        final PaletteContainer drawer = getContainerPaletteDrawer(table.getDataSource().getContainer());
        if (drawer != null) {
            for (Object entry : drawer.getChildren()) {
                if (entry instanceof ToolEntryTable && ((ToolEntryTable)entry).table == table) {
                    drawer.remove((ToolEntryTable)entry);
                    break;
                }
            }
        }
        if (table.getDataSource() != null) {
            DBPDataSourceContainer container = table.getDataSource().getContainer();
            if (container != null) {
                synchronized (usedDataSources) {
                    DataSourceInfo dataSourceInfo = usedDataSources.get(container);
                    if (dataSourceInfo == null) {
                        log.warn("Datasource '" + container + "' not registered in ERD viewer");
                    } else {
                        dataSourceInfo.tableCount--;
                        if (dataSourceInfo.tableCount <= 0) {
                            usedDataSources.remove(container);
                            releaseContainer(container);
                        }
                    }
                }
            }
        }
    }

    private void acquireContainer(DBPDataSourceContainer container)
    {
        container.acquire(editor);
        container.getRegistry().addDataSourceListener(this);

        PaletteRoot paletteRoot = editor.getPaletteRoot();

        PaletteDrawer dsDrawer = new PaletteDrawer(
            container.getName(),
            DBeaverIcons.getImageDescriptor(container.getDriver().getIcon()));
        dsDrawer.setDescription(container.getDescription());
        dsDrawer.setId(container.getId());

        paletteRoot.add(dsDrawer);
    }

    private void releaseContainer(DBPDataSourceContainer container)
    {
        PaletteDrawer drawer = getContainerPaletteDrawer(container);
        if (drawer != null) {
            editor.getPaletteRoot().remove(drawer);
        }

        container.getRegistry().removeDataSourceListener(this);
        container.release(editor);
    }

    PaletteDrawer getContainerPaletteDrawer(DBPDataSourceContainer container)
    {
        for (Object child : editor.getPaletteRoot().getChildren()) {
            if (child instanceof PaletteDrawer && container.getId().equals(((PaletteDrawer) child).getId())) {
                return (PaletteDrawer) child;
            }
        }
        return null;
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        if (!(event.getObject() instanceof DBPDataSourceContainer)) {
            return;
        }
        DBPDataSourceContainer container = (DBPDataSourceContainer)event.getObject();
        if (usedDataSources.containsKey(container) &&
            event.getAction() == DBPEvent.Action.OBJECT_UPDATE &&
            Boolean.FALSE.equals(event.getEnabled()) &&
            !DBeaverCore.isClosing())
        {
            // Close editor only if it is simple disconnect
            // Workbench shutdown doesn't close editor
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {

                    editor.getSite().getWorkbenchWindow().getActivePage().closeEditor(editor, false);
                }
            });
        }
    }

    private class ToolEntryTable extends ToolEntry {
        private final DBSEntity table;
        public ToolEntryTable(DBSEntity table)
        {
            super(table.getName(), table.getDescription(), DBeaverIcons.getImageDescriptor(DBIcon.TREE_TABLE), null);
            this.setUserModificationPermission(PERMISSION_NO_MODIFICATION);
            setDescription(DBUtils.getObjectFullName(table, DBPEvaluationContext.UI));
            this.table = table;
        }
        @Override
        public Tool createTool()
        {
            return new ToolSelectTable(table);
        }
    }

    public static class ToolSelectTable extends SelectionTool {
        private final DBSEntity table;

        public ToolSelectTable(DBSEntity table)
        {
            this.table = table;
        }

        @Override
        public void activate()
        {
            //ERDGraphicalViewer.this.reveal(part);
            DefaultEditDomain editDomain = (DefaultEditDomain) getDomain();
            final ERDEditorPart editorPart = (ERDEditorPart)editDomain.getEditorPart();
            final GraphicalViewer viewer = editorPart.getViewer();
            for (Object child : editorPart.getDiagramPart().getChildren()) {
                if (child instanceof EntityPart) {
                    if (((EntityPart)child).getTable().getObject() == table) {
                        viewer.reveal((EditPart) child);
                        viewer.select((EditPart) child);
                        break;
                    }
                }
            }
            super.activate();
        }
    }

}