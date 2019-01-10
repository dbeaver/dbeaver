/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract EditPart implementation which is property aware and responds to
 * PropertyChangeEvents fired from the model
 *
 * @author Serge Rider
 */
public abstract class PropertyAwarePart extends AbstractGraphicalEditPart implements PropertyChangeListener, DBPNamedObject {

    @NotNull
    @Override
    public String getName() {
        return ((ERDObject) getModel()).getName();
    }

    @NotNull
    public DiagramPart getDiagramPart() {
        for (EditPart part = getParent(); part != null; part = part.getParent()) {
            if (part instanceof DiagramPart) {
                return (DiagramPart) part;
            }
        }
        throw new IllegalStateException("Diagram part must be top level part");
    }

    @NotNull
    public EntityDiagram getDiagram() {
        return getDiagramPart().getDiagram();
    }

    protected boolean isEditEnabled() {
        return getDiagram().isLayoutManualAllowed();
    }

    @Override
    public void activate() {
        super.activate();
        ERDObject<?> erdObject = (ERDObject<?>) getModel();
        if (isEditEnabled()) {
            erdObject.addPropertyChangeListener(this);
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (isEditEnabled()) {
            ERDObject<?> erdObject = (ERDObject<?>) getModel();
            erdObject.removePropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        String property = evt.getPropertyName();

        switch (property) {
            case ERDObject.CHILD:
                handleChildChange(evt);
                break;
            case ERDObject.REORDER:
                handleReorderChange(evt);
                break;
            case ERDObject.OUTPUT:
                handleOutputChange(evt);
                break;
            case ERDObject.INPUT:
                handleInputChange(evt);
                break;
            case ERDObject.NAME:
                commitNameChange(evt);
                break;
        }

        //we want direct edit name changes to update immediately
        //not use the Graph animation, if automatic layout is being used
        if (ERDObject.NAME.equals(property)) {
            GraphicalEditPart graphicalEditPart = (GraphicalEditPart) (getViewer().getContents());
            IFigure partFigure = graphicalEditPart.getFigure();
            partFigure.getUpdateManager().performUpdate();
        }

    }

    /**
     * Called when change to one of the inputs occurs
     */
    private void handleInputChange(PropertyChangeEvent evt) {

        //this works but is not efficient
        //refreshTargetConnections();

        //a more efficient implementation should either remove or add the
        //relevant target connection
        //using the removeTargetConnection(ConnectionEditPart connection) or
        //addTargetConnection(ConnectionEditPart connection, int index)

        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        if (!((oldValue != null) ^ (newValue != null))) {
            throw new IllegalStateException("Exactly one of old or new values must be non-null for INPUT event");
        }

        if (newValue != null) {
            //add new connection
            ConnectionEditPart editPart = createOrFindConnection(newValue);
            int modelIndex = getModelTargetConnections().indexOf(newValue);
            addTargetConnection(editPart, modelIndex);

        } else {

            //remove connection
            List<?> children = getTargetConnections();

            ConnectionEditPart partToRemove = null;
            for (Iterator<?> iter = children.iterator(); iter.hasNext(); ) {
                ConnectionEditPart part = (ConnectionEditPart) iter.next();
                if (part.getModel() == oldValue) {
                    partToRemove = part;
                    break;
                }
            }

            if (partToRemove != null)
                removeTargetConnection(partToRemove);
        }

        getContentPane().revalidate();

    }

    /**
     * Called when change to one of the outputs occurs
     */
    private void handleOutputChange(PropertyChangeEvent evt) {

        //this works but is not efficient
        //refreshSourceConnections();

        // a more efficient implementation should either remove or add the
        // relevant target connect
        //using the removeSourceConnection(ConnectionEditPart connection) or
        //addSourceConnection(ConnectionEditPart connection, int index)

        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        if (!((oldValue != null) ^ (newValue != null))) {
            throw new IllegalStateException("Exactly one of old or new values must be non-null for INPUT event");
        }

        if (newValue != null) {
            //add new connection
            ConnectionEditPart editPart = createOrFindConnection(newValue);
            int modelIndex = getModelSourceConnections().indexOf(newValue);
            addSourceConnection(editPart, modelIndex);

        } else {

            //remove connection
            List<?> children = getSourceConnections();

            ConnectionEditPart partToRemove = null;
            for (Iterator<?> iter = children.iterator(); iter.hasNext(); ) {
                ConnectionEditPart part = (ConnectionEditPart) iter.next();
                if (part.getModel() == oldValue) {
                    partToRemove = part;
                    break;
                }
            }

            if (partToRemove != null)
                removeSourceConnection(partToRemove);
        }

        getContentPane().revalidate();

    }

    /**
     * called when child added or removed
     */
    protected void handleChildChange(PropertyChangeEvent evt) {

        //we could do this but it is not very efficient
        //refreshChildren();

        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        if ((oldValue != null) == (newValue != null)) {
            throw new IllegalStateException("Exactly one of old or new values must be non-null for CHILD event");
        }

        if (newValue != null) {
            //add new child
            EditPart editPart = createChild(newValue);
            int modelIndex = getModelChildren().indexOf(newValue);
            addChild(editPart, modelIndex);

        } else {

            List<?> children = getChildren();

            EditPart partToRemove = null;
            for (Iterator<?> iter = children.iterator(); iter.hasNext(); ) {
                EditPart part = (EditPart) iter.next();
                if (part.getModel() == oldValue) {
                    partToRemove = part;
                    break;
                }
            }

            if (partToRemove != null)
                removeChild(partToRemove);
        }

        //getContentPane().revalidate();

    }

    /**
     * Called when columns are re-ordered within
     */
    protected void handleReorderChange(PropertyChangeEvent evt) {
        refreshChildren();
        refreshVisuals();
    }

    protected void commitNameChange(PropertyChangeEvent evt) {
    }

}