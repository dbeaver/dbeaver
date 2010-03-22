package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.graphics.Image;

/**
 * GridRow
 */
public interface IGridRow
{
    int getIndex();

    int getColumn();

    void setImage(int column, Image image);

    String getText(int column);

    void setText(int column, String text);

    Object getData();

    void setData(Object data);

}
