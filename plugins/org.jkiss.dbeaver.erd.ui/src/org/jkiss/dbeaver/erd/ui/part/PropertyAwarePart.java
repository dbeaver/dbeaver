/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.draw2dl.IFigure;
import org.eclipse.draw2dl.geometry.Dimension;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.gef3.ConnectionEditPart;
import org.eclipse.gef3.EditPart;
import org.eclipse.gef3.GraphicalEditPart;
import org.eclipse.gef3.editparts.AbstractGraphicalEditPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.model.ERDObject;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;

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

    @NotNull
    public ERDEditorPart getEditor() {
        return ((ERDGraphicalViewer)getViewer()).getEditor();
    }

    @Nullable
    public DBECommandContext getCommandContext() {
        return getEditor().getCommandContext();
    }

    protected boolean isLayoutEnabled() {
        return getDiagram().isLayoutManualAllowed();
    }

    protected boolean isEditEnabled() {
        return getDiagram().isEditEnabled();
    }

    protected boolean isColumnDragAndDropSupported() {
        return true;
    }

    @Override
    public void activate() {
        super.activate();
        ERDObject<?> erdObject = (ERDObject<?>) getModel();
        if (isLayoutEnabled() || isEditEnabled()) {
            erdObject.addPropertyChangeListener(this);
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (isLayoutEnabled() || isEditEnabled()) {
            ERDObject<?> erdObject = (ERDObject<?>) getModel();
            erdObject.removePropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        String property = evt.getPropertyName();

        switch (property) {
            case ERDObject.PROP_CHILD:
                handleChildChange(evt);
                break;
            case ERDObject.PROP_REORDER:
                handleReorderChange(evt);
                break;
            case ERDObject.PROP_OUTPUT:
                handleOutputChange(evt);
                break;
            case ERDObject.PROP_INPUT:
                handleInputChange(evt);
                break;
            case ERDObject.PROP_NAME:
                commitNameChange(evt);
                break;
            case ERDObject.PROP_CONTENTS:
                commitRefresh(evt);
                break;
            case ERDObject.PROP_SIZE: {
                IFigure figure = getFigure();
                if (this instanceof NodePart) {
                    Rectangle curBounds = figure.getBounds().getCopy();
                    Dimension newSize = figure.getPreferredSize();
                    curBounds.width = newSize.width;
                    curBounds.height = newSize.height;
                    ((NodePart) this).modifyBounds(curBounds);
                } else {
                    figure.setSize(figure.getPreferredSize());
                }
                break;
            }
        }

        //we want direct edit name changes to update immediately
        //not use the Graph animation, if automatic layout is being used
        if (ERDObject.PROP_NAME.equals(property)) {
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
            throw new IllegalStateException("Exactly one of old or new values must be non-null for PROP_INPUT event");
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
            throw new IllegalStateException("Exactly one of old or new values must be non-null for PROP_INPUT event");
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
            throw new IllegalStateException("Exactly one of old or new values must be non-null for PROP_CHILD event");
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

    // Refresh part name
    protected void commitNameChange(PropertyChangeEvent evt) {
    }

    // Refresh part contents
    protected void commitRefresh(PropertyChangeEvent evt) {
        commitNameChange(evt);
        refreshChildren();
        refreshVisuals();
    }

    @Override
    public Object getAdapter(Class key) {
        if (key == IPropertySource.class) {
            Object model = getModel();
            if (model instanceof ERDObject) {
                Object object = ((ERDObject) model).getObject();
                if (object instanceof DBSObject) {
                    if (isEditEnabled()) {
                        DBECommandContext commandContext = getCommandContext();
                        if (commandContext != null) {
                            PropertySourceEditable pse = new PropertySourceEditable(commandContext, object, object);
                            pse.collectProperties();
                            return new PropertySourceDelegate(pse);
                        }
                    }
                    PropertyCollector propertyCollector = new PropertyCollector(object, false);
                    propertyCollector.collectProperties();
                    return new PropertySourceDelegate(propertyCollector);
                }
            }
            return null;
        }
        return super.getAdapter(key);
    }
}