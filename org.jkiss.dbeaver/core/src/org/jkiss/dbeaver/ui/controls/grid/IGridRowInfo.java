package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.graphics.Image;

/**
 * GridRow info
 */
public interface IGridRowInfo
{
    String getText();

    void setText(String text);

    String getToolTip();

    void setToolTip(String text);

    Image getImage();

    void setImage(Image image);

}