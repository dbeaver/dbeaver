/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public interface IGridContentProvider {

    /**
     * Row count
     * @return row count
     */
    public int getRowCount();

    public static final int CS_NONE = 0;
    public static final int CS_SORTABLE = 1;
    public static final int CS_RESIZABLE = 1 >> 1;
    public static final int CS_MOVEABLE = 1 >> 1;

    ////////////////////////////////////
    // Columns
    ////////////////////////////////////

    public int getColumnCount();

    public String getColumnText(int index);

    public String getColumnToolTip(int index);

    public Color getColumnBackground(int index);

    public Color getColumnForeground(int index);

    public Image getColumnImage(int index);

    public int getColumnStyle(int index);

    public int getColumnSize(int index);

    ////////////////////////////////////
    // Header
    ////////////////////////////////////

    public String getHeaderText(int row);

    public String getHeaderToolTip(int row);

    public Image getHeaderImage(int row);

    public Color getHeaderBackground(int row);

    public Color getHeaderForeground(int row);

    ////////////////////////////////////
    // Cells
    ////////////////////////////////////

	/**
	 * Returns the background color at the given column index in the receiver.
	 *
	 * @param column
	 *            the column index
	 * @return the background color
	 */
	public Color getCellBackground(int row, int column);

    /**
     * Returns the foreground color at the given column index in the receiver.
     *
     * @param column
     *            the column index
     * @return the foreground color
     */
    public Color getCellForeground(int row, int column);

	/**
	 * Returns the font that the receiver will use to paint textual information
	 * for the specified cell in this item.
	 *
	 * @param column
	 *            the column index
	 * @return the receiver's font
	 */
	public Font getCellFont(int row, int column);

	/**
	 * Returns the image stored at the given column index in the receiver, or
	 * null if the image has not been set or if the column does not exist.
	 *
	 * @param column
	 *            the column index
	 * @return the image stored at the given column index in the receiver
	 */
	public Image getCellImage(int row, int column);

	/**
	 * Returns the text stored at the given column index in the receiver, or
	 * empty string if the text has not been set.
	 *
	 * @param column
	 *            the column index
	 * @return the text stored at the given column index in the receiver
	 */
	public String getCellText(int row, int column);

    /**
     * Returns the tooltip for the given cell.
     *
     * @param column
     *            the column index
     * @return the tooltip
     */
    public String getCellToolTipText(int row, int column);

}
