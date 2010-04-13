/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEffect;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;


/**
 * This class provides default implementations to display a source image
 * when a drag is initiated from a <code>Grid</code>.
 * 
 * <p>Classes that wish to provide their own source image for a <code>Grid</code> can
 * extend <code>DragSourceAdapter</code> class and override the <code>DragSourceAdapter.dragStart</code>
 * method and set the field <code>DragSourceEvent.image</code> with their own image.</p>
 *
 * Subclasses that override any methods of this class must call the corresponding
 * <code>super</code> method to get the default drag under effect implementation.
 *
 * @see DragSourceAdapter
 * @see DragSourceEvent
 * 
 * @since 3.3
 * @author mark-oliver.reiser
 */
public class GridDragSourceEffect extends DragSourceEffect {

	Image dragSourceImage = null;

	/**
	 * 
	 * @param grid
	 */
	public GridDragSourceEffect(Grid grid) {
		super(grid);
	}

	/**
	 * This implementation of <code>dragFinished</code> disposes the image
	 * that was created in <code>GridDragSourceEffect.dragStart</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragFinished(event)</code>
	 * to dispose the image in the default implementation.
	 * 
	 * @param event the information associated with the drag finished event
	 */
	public void dragFinished(DragSourceEvent event) {
		if (dragSourceImage != null) dragSourceImage.dispose();
		dragSourceImage = null;
	}

	/**
	 * This implementation of <code>dragStart</code> will create a default
	 * image that will be used during the drag. The image should be disposed
	 * when the drag is completed in the <code>GridDragSourceEffect.dragFinished</code>
	 * method.
	 * 
	 * Subclasses that override this method should call <code>super.dragStart(event)</code>
	 * to use the image from the default implementation.
	 * 
	 * @param event the information associated with the drag start event
	 */
	public void dragStart(DragSourceEvent event) {
		event.image = getDragSourceImage(event);
	}

	Image getDragSourceImage(DragSourceEvent event) {
		if (dragSourceImage != null) dragSourceImage.dispose();
		dragSourceImage = null;		
		Grid grid = (Grid) getControl();
		Display display = grid.getDisplay();
		Rectangle empty = new Rectangle(0,0,0,0);
		
		// Collect the currently selected items. 
		Point[] selection;
		if(grid.getCellSelectionEnabled()){
			selection = grid.getCellSelection();
		} else {
			List l = new ArrayList();
			GridItem[] selItems = grid.getSelection();
			for (int i = 0; i < selItems.length; i++){
				for (int j = 0; j < grid.getColumnCount() ; j++){
					if(grid.getColumn(j).isVisible()){
						l.add(new Point(j,grid.indexOf(selItems[i])));
					}
				}
			}
			selection = (Point[])l.toArray(new Point[l.size()]);
		}
		if (selection.length == 0) return null;
		
		Rectangle bounds=null;
		for (int i = 0; i < selection.length; i++) {
			GridItem item = grid.getItem(selection[i].y);
			Rectangle currBounds = item.getBounds(selection[i].x);
			
			if(empty.equals(currBounds)){
				selection[i]=null;
			}else {
				if(bounds==null){
					bounds = currBounds;
				}else {
					bounds = bounds.union(currBounds);
				}
			}
		}
		if(bounds==null) return null;
		if (bounds.width <= 0 || bounds.height <= 0) return null;
		
		dragSourceImage = new Image(display,bounds.width,bounds.height);
		GC gc = new GC(dragSourceImage);
		for (int i = 0; i < selection.length; i++) {
			if(selection[i]==null) continue;
			GridItem item = grid.getItem(selection[i].y);
			GridColumn column = grid.getColumn(selection[i].x);
			Rectangle currBounds = item.getBounds(selection[i].x);
			GridCellRenderer r = column.getCellRenderer();
			r.setBounds(currBounds.x-bounds.x, currBounds.y-bounds.y, currBounds.width, currBounds.height);
            gc.setClipping(currBounds.x-bounds.x-1, currBounds.y-bounds.y-1, currBounds.width+2, currBounds.height+2);
			r.setColumn(selection[i].x);
            r.setSelected(false);
            r.setFocus(false);
            r.setRowFocus(false);
            r.setCellFocus(false);
            r.setRowHover(false);
            r.setColumnHover(false);
            r.setCellSelected(false);                            
            r.setHoverDetail("");
            r.setDragging(true);
            r.paint(gc, item);
            gc.setClipping((Rectangle)null);
		}
		gc.dispose();
		
		return dragSourceImage;
	}
}
