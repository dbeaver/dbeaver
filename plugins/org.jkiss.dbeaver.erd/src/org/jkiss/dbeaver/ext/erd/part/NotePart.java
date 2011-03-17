/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.DirectEditManager;
import org.jkiss.dbeaver.ext.erd.figures.NoteFigure;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.policy.NoteEditPolicy;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;

import java.beans.PropertyChangeEvent;

/**
 * Represents the editable/resizable note.
 * 
 * @author Serge Rieder
 */
public class NotePart extends NodePart
{

	protected DirectEditManager manager;

	public ERDNote getNote()
	{
		return (ERDNote) getModel();
	}

	/**
	 * Creates edit policies and associates these with roles
	 */
	protected void createEditPolicies()
	{
        final boolean editEnabled = isEditEnabled();
        if (editEnabled) {
            //installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new EntityNodeEditPolicy());
            //installEditPolicy(EditPolicy.LAYOUT_ROLE, new EntityLayoutEditPolicy());
            //installEditPolicy(EditPolicy.CONTAINER_ROLE, new EntityContainerEditPolicy());
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new NoteEditPolicy());
            //installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new NoteDirectEditPolicy());

            //installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new ResizableEditPolicy());
        }
	}

	public void performRequest(Request request)
	{
		if (request.getType() == RequestConstants.REQ_DIRECT_EDIT)
		{
/*
			if (request instanceof DirectEditRequest
					&& !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy()))
				return;
			performDirectEdit();
*/
		} else if (request.getType() == RequestConstants.REQ_OPEN) {
            final String newText = EditTextDialog.editText(getViewer().getControl().getShell(), "Note", getNote().getObject());
            if (newText != null) {
                getNote().setObject(newText);
                ((NoteFigure)getFigure()).setText(newText);
            }
            //getTable().openEditor();
        }
	}

/*
	private boolean directEditHitTest(Point requestLoc)
	{
		NoteFigure figure = (NoteFigure) getFigure();
		figure.translateToRelative(requestLoc);
        return figure.containsPoint(requestLoc);
    }

	protected void performDirectEdit()
	{
		if (manager == null)
		{
			ERDGraphicalViewer viewer = (ERDGraphicalViewer) getViewer();
			ValidationMessageHandler handler = viewer.getValidationHandler();

			NoteFigure figure = (NoteFigure) getFigure();
			manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(figure),
					figure, null);
		}
		manager.show();
	}
*/

	//******************* Miscellaneous stuff *********************/

	public String toString()
	{
		return getNote().getObject();
	}

	//******************* Listener related methods *********************/

    public void handleNameChange(String value)
    {
        NoteFigure noteFigure = (NoteFigure) getFigure();
        noteFigure.setVisible(false);
        refreshVisuals();
    }

    /**
     * Reverts to existing name in model when exiting from a direct edit
     * (possibly before a commit which will result in a change in the label
     * value)
     */
    public void revertNameChange()
    {
        NoteFigure noteFigure = (NoteFigure) getFigure();
        noteFigure.setText(getNote().getObject());
        noteFigure.setVisible(true);
        refreshVisuals();
    }

	/**
	 * Handles change in name when committing a direct edit
	 */
	protected void commitNameChange(PropertyChangeEvent evt)
	{
		NoteFigure noteFigure = (NoteFigure) getFigure();
		noteFigure.setText(getNote().getObject());
		noteFigure.setVisible(true);
		refreshVisuals();
	}

	//******************* Layout related methods *********************/

	/**
	 * Creates a figure which represents the table
	 */
	protected IFigure createFigure()
	{
        final NoteFigure noteFigure = new NoteFigure(getNote());
        Rectangle bounds = ((DiagramPart) getParent()).getDiagram().getInitBounds(getNote());
        if (bounds != null) {
            noteFigure.setBounds(bounds);
            noteFigure.setPreferredSize(bounds.getSize());

            //noteFigure.setLocation(bounds.getLocation());
            //noteFigure.setSize(bounds.getSize());
        } else if (noteFigure.getSize().isEmpty()) {
            noteFigure.setPreferredSize(new Dimension(100, 50));
        }
        return noteFigure;
    }

	/**
	 * Reset the layout constraint, and revalidate the content pane
	 */
	protected void refreshVisuals()
	{
		NoteFigure notefigure = (NoteFigure) getFigure();
		Point location = notefigure.getLocation();
		DiagramPart parent = (DiagramPart) getParent();
		Rectangle constraint = new Rectangle(location.x, location.y, -1, -1);
		parent.setLayoutConstraint(this, notefigure, constraint);
	}

	/**
	 * Sets the width of the line when selected
	 */
	public void setSelected(int value)
	{
		super.setSelected(value);
/*
		NoteFigure noteFigure = (NoteFigure) getFigure();
		if (value != EditPart.SELECTED_NONE)
			noteFigure.setSelected(true);
		else
			noteFigure.setSelected(false);
		noteFigure.repaint();
*/
	}

    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection)
    {
        return new ChopboxAnchor(getFigure());
    }

    public ConnectionAnchor getSourceConnectionAnchor(Request request)
    {
        return new ChopboxAnchor(getFigure());
    }

    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection)
    {
        return new ChopboxAnchor(getFigure());
    }

    public ConnectionAnchor getTargetConnectionAnchor(Request request)
    {
        return new ChopboxAnchor(getFigure());
    }
}