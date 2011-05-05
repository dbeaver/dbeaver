/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides base class support for model objects to participate in event handling framework
 * @author Serge Rieder
 */
public abstract class ERDObject<OBJECT> implements IAdaptable, DBPNamedObject
{
    static final Log log = LogFactory.getLog(ERDObject.class);

	public static final String CHILD = "CHILD";
	public static final String REORDER = "REORDER";
	public static final String INPUT = "INPUT";
	public static final String OUTPUT = "OUTPUT";
	public static final String NAME = "NAME";

	protected transient PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    protected OBJECT object;
    private PropertyCollector propertyCollector = null;

	protected ERDObject(OBJECT object)
	{
        this.object = object;
	}

    public OBJECT getObject() {
        return object;
    }

    public void setObject(OBJECT object)
    {
        this.object = object;
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

    public void openEditor() {
        if (object instanceof DBSObject) {
            DBeaverCore.runUIJob("Open object editor", new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(
                        monitor,
                        (DBSObject) object,
                        true
                    );
                    if (node != null) {
                        NavigatorHandlerObjectOpen.openEntityEditor(node, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                }
            });
        }
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySource.class) {
            return getPropertyCollector();
        }
        return null;
    }
}

