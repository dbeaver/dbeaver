/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides base class support for model objects to participate in event handling framework
 * @author Serge Rieder
 */
public abstract class ERDObject<OBJECT> implements IAdaptable, DBPNamedObject
{
    private static final Log log = Log.getLog(ERDObject.class);

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
            DBeaverUI.runUIJob("Open object editor", new DBRRunnableWithProgress() {
                @Override
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

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == DBPPropertySource.class) {
            return getPropertyCollector();
        }
        return null;
    }
}

