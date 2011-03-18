/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * GraphicalViewer which also knows about ValidationMessageHandler to output
 * error messages to
 * @author Serge Rieder
 */
public class ERDGraphicalViewer extends ScrollingGraphicalViewer implements IPropertyChangeListener, DBPEventListener {
    static final Log log = LogFactory.getLog(ERDGraphicalViewer.class);

    private ERDEditorPart editor;
	private ValidationMessageHandler messageHandler;
    private IThemeManager themeManager;

    private static class DataSourceInfo {
        int tableCount = 0;
    }

    private final Map<DBSDataSourceContainer, DataSourceInfo> usedDataSources = new IdentityHashMap<DBSDataSourceContainer, DataSourceInfo>();

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
            IFocusService fs = (IFocusService) PlatformUI.getWorkbench().getService(IFocusService.class);
            fs.addFocusTracker(control, editor.getEditorInput() + "#" + this.hashCode());

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
            IFocusService fs = (IFocusService) PlatformUI.getWorkbench().getService(IFocusService.class);
            fs.removeFocusTracker(getControl());
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
	protected void handleFocusLost(FocusEvent fe)
	{
		//give the superclass a chance to handle this first
		super.handleFocusLost(fe);
		//call reset on the MessageHandler itself
		messageHandler.reset();
	}

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
        editor.refreshDiagram();
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
        super.setContents(editpart);
    }

    @Override
    public void setContents(Object contents)
    {
        super.setContents(contents);
    }

    public void handleTableActivate(DBSTable table)
    {
        if (table.getDataSource() != null) {
            DBSDataSourceContainer container = table.getDataSource().getContainer();
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
    }

    public void handleTableDeactivate(DBSTable table)
    {
        if (table.getDataSource() != null) {
            DBSDataSourceContainer container = table.getDataSource().getContainer();
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

    private void acquireContainer(DBSDataSourceContainer container)
    {
        container.acquire(editor);
        container.getRegistry().addDataSourceListener(this);
    }

    private void releaseContainer(DBSDataSourceContainer container)
    {
        container.getRegistry().removeDataSourceListener(this);
        container.release(editor);
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        if (!(event.getObject() instanceof DBSDataSourceContainer)) {
            return;
        }
        DBSDataSourceContainer container = (DBSDataSourceContainer)event.getObject();
        if (usedDataSources.containsKey(container) &&
            event.getAction() == DBPEvent.Action.OBJECT_UPDATE &&
            Boolean.FALSE.equals(event.getEnabled()) &&
            !DBeaverCore.getInstance().isClosing())
        {
            // Close editor only if it is simple disconnect
            // Workbench shutdown doesn't close editor
            Display.getDefault().asyncExec(new Runnable() {
                public void run()
                {

                    editor.getSite().getWorkbenchWindow().getActivePage().closeEditor(editor, false);
                }
            });
        }
    }

}