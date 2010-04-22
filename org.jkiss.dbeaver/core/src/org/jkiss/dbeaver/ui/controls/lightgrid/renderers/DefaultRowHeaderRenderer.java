/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridItem;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The row header renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultRowHeaderRenderer extends AbstractRenderer
{

    int leftMargin = 6;

    int rightMargin = 8;

    int topMargin = 3;

    int bottomMargin = 3;

    private TextLayout textLayout;

    public DefaultRowHeaderRenderer(LightGrid grid) {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        GridItem item = (GridItem) value;

        String text = getHeaderText(item);

        gc.setFont(getDisplay().getSystemFont());
        
        Color background = getHeaderBackground(item);
        if( background == null ) {
        	background = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        }
        gc.setBackground(background);

        if (isSelected())
        {
            gc.setBackground(item.getParent().getCellHeaderSelectionBackground());
        }

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width, getBounds().height + 1);


        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                                                                               + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
        }

        int x = leftMargin;

        Image image = getHeaderImage(item);

        if( image != null ) {
            gc.drawImage(image, x, getBounds().y + (getBounds().height - image.getBounds().height)/2);
        	x += image.getBounds().width + 5;
        }

        int width = getBounds().width - x;

        width -= rightMargin;

        Color foreground = getHeaderForeground(item);
        if( foreground == null ) {
        	foreground = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        }
        
        gc.setForeground(foreground);

        
        int y = getBounds().y;
        int selectionOffset = 0;
        
        if (!item.getParent().isWordWrapHeader())
        {
            y += (getBounds().height - gc.stringExtent(text).y) / 2;
            gc.drawString(TextUtils.getShortString(gc, text, width), getBounds().x + x + selectionOffset, y + selectionOffset, true);
        }
        else
        {
          getTextLayout(gc, item);
          textLayout.setWidth(width < 1 ? 1 : width);
          textLayout.setText(text);
          
          textLayout.draw(gc, getBounds().x + x + selectionOffset, y + selectionOffset);
        }

    }

    private Image getHeaderImage(GridItem item) {
    	return item.getHeaderImage();
    }

    private String getHeaderText(GridItem item)
    {
        String text = item.getHeaderText();
        if (text == null)
        {
            text = (item.getParent().indexOf(item) + 1) + "";
        }
        return text;
    }
    
    private Color getHeaderBackground(GridItem item) {
    	return item.getHeaderBackground();
    }
    
    private Color getHeaderForeground(GridItem item) {
    	return item.getHeaderForeground();
    }
    
    private void getTextLayout(GC gc, GridItem gridItem)
    {
        if (textLayout == null)
        {
            textLayout = new TextLayout(gc.getDevice());
            textLayout.setFont(gc.getFont());
            gridItem.getParent().addDisposeListener(new DisposeListener()
            {                
                public void widgetDisposed(DisposeEvent e)
                {
                    textLayout.dispose();
                }                
            });
        }
    }
}
