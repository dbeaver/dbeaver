/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2dl.ChopboxAnchor;
import org.eclipse.draw2dl.ConnectionAnchor;
import org.eclipse.draw2dl.geometry.Dimension;
import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.gef3.*;
import org.eclipse.gef3.commands.Command;
import org.eclipse.gef3.requests.DirectEditRequest;
import org.eclipse.gef3.tools.DirectEditManager;
import org.jkiss.dbeaver.erd.model.ERDElement;
import org.jkiss.dbeaver.erd.model.ERDNote;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.directedit.ExtendedDirectEditManager;
import org.jkiss.dbeaver.erd.ui.directedit.FigureEditorLocator;
import org.jkiss.dbeaver.erd.ui.figures.NoteFigure;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.policy.EntityConnectionEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.NoteDirectEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.NoteEditPolicy;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.controls.MultilineTextCellEditor;

import java.beans.PropertyChangeEvent;

/**
 * Represents the editable/resizable note.
 * 
 * @author Serge Rider
 */
public class NotePart extends NodePart
{
    private DirectEditManager manager;

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
        final boolean layoutEnabled = isLayoutEnabled();
        if (layoutEnabled) {
            installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new EntityConnectionEditPolicy());
            //installEditPolicy(EditPolicy.LAYOUT_ROLE, new EntityLayoutEditPolicy());
            //installEditPolicy(EditPolicy.CONTAINER_ROLE, new EntityContainerEditPolicy());
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new NoteEditPolicy());
            installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new NoteDirectEditPolicy());
            //installEditPolicy(EditPolicy.COMPONENT_ROLE, new NoteDirectEditPolicy());

            //installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new ResizableEditPolicy());
        }

        getDiagram().getModelAdapter().installPartEditPolicies(this);
	}

    @Override
    public EditPart getTargetEditPart(Request request) {
        if (RequestConstants.REQ_CONNECTION_START.equals(request.getType()) ||
            RequestConstants.REQ_CONNECTION_END.equals(request.getType()))
        {
            return this;
        }
        return super.getTargetEditPart(request);
    }

	@Override
    public void performRequest(Request request)
	{
		if (request.getType() == RequestConstants.REQ_OPEN) {
            performDirectEdit();
        } else if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
			if (request instanceof DirectEditRequest
					&& !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy()))
				return;
			performDirectEdit();
        } else {
            getDiagram().getModelAdapter().performPartRequest(this, request);
        }
	}

    @Override
    public Command getCommand(Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
            performDirectEdit();
        }
        return super.getCommand(request);
    }

    private boolean directEditHitTest(Point requestLoc) {
        NoteFigure figure = (NoteFigure) getFigure();
        figure.translateToRelative(requestLoc);
        return figure.containsPoint(requestLoc);
    }

    private void performDirectEdit() {
        if (manager == null) {
            NoteFigure figure = (NoteFigure) getFigure();
            manager = new ExtendedDirectEditManager(
                this,
                MultilineTextCellEditor.class,
                new FigureEditorLocator(figure),
                figure,
                value -> null);
        }
        manager.show();
    }

	public String toString()
	{
		return getNote().getObject();
	}

    public void handleNameChange(String value)
    {
        NoteFigure noteFigure = (NoteFigure) getFigure();
        noteFigure.setVisible(false);
        refreshVisuals();
    }

    /**
     * Reverts to existing name in model when exiting from a direct edit
     * (possibly before a commit which will result in a change in the figure
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
    protected NoteFigure createFigure()
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
        if (visualInfo.transparent) {
            noteFigure.setOpaque(false);
        }
        if (visualInfo.bgColor != null) {
            noteFigure.setBackgroundColor(visualInfo.bgColor);
        }
        if (visualInfo.fgColor != null) {
            noteFigure.setForegroundColor(visualInfo.fgColor);
        }
        if (visualInfo.borderWidth != ERDUIConstants.DEFAULT_NOTE_BORDER_WIDTH) {
            noteFigure.setBorder(createBorder(visualInfo.borderWidth));
        }
        if (visualInfo.font != null) {
            noteFigure.setFont(visualInfo.font);
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

    @Override
    public ERDElement getElement() {
        return getNote();
    }
}