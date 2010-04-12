package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * MenuContributionItem
 */
public abstract class MenuContributionItem extends ContributionItem implements IPropertyChangeListener
{
    private IAction action;
    private String text;
    private Image image;
    private String toolTip;
    private ToolItem toolItem;

    protected MenuContributionItem()
    {
    }

    protected MenuContributionItem(String text, Image image, String toolTip)
    {
        this.text = text;
        this.image = image;
        this.toolTip = toolTip;
    }

    protected MenuContributionItem(IAction action)
    {
        this.action = action;
        if (action.getImageDescriptor() != null) {
            this.image = action.getImageDescriptor().createImage();
        } else {
            this.text = action.getText();
        }
        this.toolTip = action.getToolTipText();

/*
        ExternalActionManager.ICallback callback = ExternalActionManager.getInstance().getCallback();
        String commandId = action.getActionDefinitionId();
        if ((callback != null) && (commandId != null) && (toolTip != null)) {
            String acceleratorText = callback.getAcceleratorText(commandId);
            if (acceleratorText != null && acceleratorText.length() != 0) {
                toolTip = JFaceResources.format(
                        "Toolbar_Tooltip_Accelerator", //$NON-NLS-1$
                        new Object[] { toolTip, acceleratorText });
            }
        }
*/

        action.addPropertyChangeListener(this);
    }

    public void dispose()
    {
        if (action != null) {
            if (image != null) {
                image.dispose();
                image = null;
            }
            action.removePropertyChangeListener(this);
        }
        super.dispose();
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(Action.ENABLED)) {
            setEnabled((Boolean)event.getNewValue());
        }
    }

    public void setEnabled(boolean enabled)
    {
        if (toolItem != null) {
            toolItem.setEnabled(enabled);
        }
    }

    public void setImage(Image image)
    {
        this.image = image;
        if (toolItem != null) {
            toolItem.setImage(image);
        }
    }

    public final void fill(final ToolBar parent, int index) {
        toolItem = new ToolItem(parent, SWT.DROP_DOWN, index);
        if (text != null) {
            toolItem.setText(text);
        }
        if (image != null) {
            toolItem.setImage(image);
        }
        if (toolTip != null) {
            toolItem.setToolTipText(toolTip);
        }
        toolItem.addListener (SWT.Selection, new Listener() {
            public void handleEvent (Event event) {
                if (event.detail == SWT.ARROW) {
                    Rectangle rect = toolItem.getBounds ();
                    Point pt = new Point(rect.x, rect.y + rect.height);
                    pt = parent.toDisplay (pt);
                    Menu menu = createMenu(parent);
                    menu.setLocation (pt.x, pt.y);
                    menu.setVisible (true);
                } else {
                    buttonPressed();
                }
            }
        });
    }

    protected void buttonPressed()
    {
        if (action != null) {
            action.run();
        }
    }

    protected abstract Menu createMenu(Control parent);

}
