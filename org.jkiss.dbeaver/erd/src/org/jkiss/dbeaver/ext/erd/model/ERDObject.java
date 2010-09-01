/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.OpenObjectEditorAction;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides base class support for model objects to participate in event handling framework
 * @author Serge Rieder
 */
public abstract class ERDObject<OBJECT extends DBSObject> implements IPropertySource
{

	public static final String CHILD = "CHILD";
	public static final String REORDER = "REORDER";
	public static final String INPUT = "INPUT";
	public static final String OUTPUT = "OUTPUT";
	public static final String NAME = "NAME";

	protected transient PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    protected final OBJECT object;
    protected PropertyCollector propertyCollector = null;

	protected ERDObject(OBJECT object)
	{
        this.object = object;
	}

    public OBJECT getObject() {
        return object;
    }

    private PropertyCollector getPropertyCollector()
    {
        if (propertyCollector == null) {
            propertyCollector = new PropertyCollector(object, false);
            if (object != null) {
                propertyCollector.collectProperties();
            }
        }
        return propertyCollector;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
	{
		listeners.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		listeners.removePropertyChangeListener(l);
	}
	
	protected void firePropertyChange(String prop, Object old, Object newValue)
	{
		listeners.firePropertyChange(prop, old, newValue);
	}

    public Object getEditableValue() {
        return getPropertyCollector().getEditableValue();
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        return getPropertyCollector().getPropertyDescriptors();
    }

    public Object getPropertyValue(Object id) {
        return getPropertyCollector().getPropertyValue(id);
    }

    public boolean isPropertySet(Object id) {
        return getPropertyCollector().isPropertySet(id);
    }

    public void resetPropertyValue(Object id) {
        getPropertyCollector().resetPropertyValue(id);
    }

    public void setPropertyValue(Object id, Object value) {
        getPropertyCollector().setPropertyValue(id, value);
    }


    public void openEditor() {
        DBeaverCore.runUIJob("Open object editor", new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                DBNNode node = DBeaverCore.getInstance().getMetaModel().getNodeByObject(
                    monitor,
                    object,
                    true
                );
                if (node != null) {
                    OpenObjectEditorAction.openEntityEditor(node, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                }
            }
        });
    }

}

