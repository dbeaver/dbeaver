package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.swt.graphics.Image;

/**
 * GridRow
 */
public interface IGridRowData
{
    int getIndex();

    int getColumn();

    void setImage(int column, Image image);

    String getText(int column);

    void setText(int column, String text);

    void setHeaderText(String text);

    void setHeaderImage(Image image);

    public void setModified(int column, boolean modified);

    public void setEmpty(int column, boolean empty);

    Object getData();

    void setData(Object data);

}
