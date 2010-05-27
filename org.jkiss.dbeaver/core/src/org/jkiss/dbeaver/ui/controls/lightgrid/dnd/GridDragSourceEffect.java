/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.dnd;

import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEffect;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridCellRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


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
	public GridDragSourceEffect(LightGrid grid) {
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
		LightGrid grid = (LightGrid) getControl();
		Display display = grid.getDisplay();
		Rectangle empty = new Rectangle(0,0,0,0);
		
		// Collect the currently selected items. 
		Collection<GridPos> selection;
        selection = new ArrayList<GridPos>(grid.getCellSelection());
		if (selection.isEmpty()) return null;
		
		Rectangle bounds=null;
		for (Iterator<GridPos> cellIter = selection.iterator(); cellIter.hasNext(); ) {
            GridPos cell = cellIter.next();
			Rectangle currBounds = grid.getCellBounds(cell.col, cell.row);
			
			if(empty.equals(currBounds)){
				cellIter.remove();
			} else {
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
		for (GridPos cell : selection) {
			GridColumn column = grid.getColumn(cell.col);
			Rectangle currBounds = grid.getCellBounds(cell.col, cell.row);
			GridCellRenderer r = column.getCellRenderer();
			r.setBounds(currBounds.x-bounds.x, currBounds.y-bounds.y, currBounds.width, currBounds.height);
            gc.setClipping(currBounds.x-bounds.x-1, currBounds.y-bounds.y-1, currBounds.width+2, currBounds.height+2);
			r.setColumn(cell.col);
            r.setRow(cell.row);
            r.setSelected(false);
            r.setFocus(false);
            r.setRowFocus(false);
            r.setCellFocus(false);
            r.setRowHover(false);
            r.setColumnHover(false);
            r.setCellSelected(false);                            
            r.setHoverDetail("");
            r.setDragging(true);
            r.paint(gc);
            gc.setClipping((Rectangle)null);
		}
		gc.dispose();
		
		return dragSourceImage;
	}
}
