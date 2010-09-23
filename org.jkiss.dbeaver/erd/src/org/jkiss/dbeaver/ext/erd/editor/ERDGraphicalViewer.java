/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;

/**
 * GraphicalViewer which also knows about ValidationMessageHandler to output
 * error messages to
 * @author Serge Rieder
 */
public class ERDGraphicalViewer extends ScrollingGraphicalViewer implements IPropertyChangeListener
{

    private ERDEditor editor;
	private ValidationMessageHandler messageHandler;
    private IThemeManager themeManager;

	/**
	 * ValidationMessageHandler to receive messages
	 * @param messageHandler message handler 
	 */
	public ERDGraphicalViewer(ERDEditor editor, ValidationMessageHandler messageHandler)
	{
		super();
        this.editor = editor;
		this.messageHandler = messageHandler;

        themeManager = editor.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(this);
	}

    @Override
    public void setControl(Control control)
    {
        super.setControl(control);

        if (control != null) {
            ERDEditorAdapter.mapControl(control, editor);
            IFocusService fs = (IFocusService) PlatformUI.getWorkbench().getService(IFocusService.class);
            fs.addFocusTracker(control, editor.getObjectManager().getObject().getObjectId() + "#" + this.hashCode());

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

}