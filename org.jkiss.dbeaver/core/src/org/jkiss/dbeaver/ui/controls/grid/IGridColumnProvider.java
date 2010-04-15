/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

public interface IGridColumnProvider {

    public int getColumnCount();

    public String getColumnText(int index);

    public String getColumnToolTipText(int index);

    public Color getColumnBackground(int index);

    public Color getColumnForeground(int index);

    public Image getColumnImage(int index);

    public boolean isColumnSortable();

    public boolean isColumnSizeable(int index);

    public int getColumnSize(int index);
}