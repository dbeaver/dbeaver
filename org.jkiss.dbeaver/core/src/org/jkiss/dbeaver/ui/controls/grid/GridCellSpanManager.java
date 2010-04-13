/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Rectangle;

class GridCellSpanManager {
	List listOfCellSpanRectangles = new ArrayList();
	Rectangle lastUsedCellSpanRectangle = null;

	protected void addCellSpanInfo(int colIndex, int rowIndex, int colSpan,
			int rowSpan) {
		Rectangle rect = new Rectangle(colIndex, rowIndex, colSpan + 1,
				rowSpan + 1);
		this.listOfCellSpanRectangles.add(rect);
	}

	private Rectangle findSpanRectangle(int columnIndex, int rowIndex) {
		Iterator iter = listOfCellSpanRectangles.iterator();
		while (iter.hasNext()) {
			Rectangle cellSpanRectangle = (Rectangle) iter.next();
			if (cellSpanRectangle.contains(columnIndex, rowIndex)) {
				return cellSpanRectangle;
			}
		}
		return null;
	}

	protected boolean skipCell(int columnIndex, int rowIndex) {
		this.lastUsedCellSpanRectangle = this.findSpanRectangle(columnIndex,
				rowIndex);
		return this.lastUsedCellSpanRectangle != null;
	}

	protected void consumeCell(int columnIndex, int rowIndex) {
		Rectangle rectangleToConsume = null;

		if (this.lastUsedCellSpanRectangle != null
				&& this.lastUsedCellSpanRectangle.contains(columnIndex,
						rowIndex)) {
			rectangleToConsume = this.lastUsedCellSpanRectangle;
		} else {
			rectangleToConsume = this.findSpanRectangle(columnIndex, rowIndex);
		}

		if (rectangleToConsume != null) {
			if (columnIndex >= rectangleToConsume.x
					+ (rectangleToConsume.width - 1)
					&& rowIndex >= (rectangleToConsume.y
							+ rectangleToConsume.height - 1)) {
				this.listOfCellSpanRectangles.remove(rectangleToConsume);
			}
		}
	}
}
