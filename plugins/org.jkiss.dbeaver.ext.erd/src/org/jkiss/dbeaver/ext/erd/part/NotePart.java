/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.figures.NoteFigure;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.policy.NoteEditPolicy;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;

import java.beans.PropertyChangeEvent;

/**
 * Represents the editable/resizable note.
 * 
 * @author Serge Rider
 */
public class NotePart extends NodePart
{
    public NotePart() {
    }

    public ERDNote getNote()
	{
		return (ERDNote) getModel();
	}

	/**
	 * Creates edit policies and associates these with roles
	 */
	@Override
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

	@Override
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
            final String newText = EditTextDialog.editText(getViewer().getControl().getShell(), ERDMessages.part_note_title, getNote().getObject());
            if (newText != null) {
                getNote().setObject(newText);
                ((NoteFigure)getFigure()).setText(newText);
            }
            //getTable().openEditor();
        }
	}

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
	@Override
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
	@Override
    protected IFigure createFigure()
	{
        final NoteFigure noteFigure = new NoteFigure(getNote());
        EntityDiagram.NodeVisualInfo visualInfo = ((DiagramPart) getParent()).getDiagram().getVisualInfo(getNote(), true);
        Rectangle bounds = visualInfo.initBounds;
        if (bounds != null) {
            noteFigure.setBounds(bounds);
            noteFigure.setPreferredSize(bounds.getSize());

            //noteFigure.setLocation(bounds.getLocation());
            //noteFigure.setSize(bounds.getSize());
        } else if (noteFigure.getSize().isEmpty()) {
            noteFigure.setPreferredSize(new Dimension(100, 50));
        }
        this.customBackground = visualInfo.bgColor;
        if (this.customBackground != null) {
            noteFigure.setBackgroundColor(this.customBackground);
        }
        return noteFigure;
    }

	/**
	 * Reset the layout constraint, and revalidate the content pane
	 */
	@Override
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
	@Override
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

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection)
    {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request)
    {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection)
    {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request)
    {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public Object getAdapter(Class key)
    {
        if (key == DBPNamedObject.class) {
            return getNote();
        }
        return super.getAdapter(key);
    }

}