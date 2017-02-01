/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides base class support for model objects to participate in event handling framework
 * @author Serge Rider
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
                        NavigatorUtils.openNavigatorNode(node, DBeaverUI.getActiveWorkbenchWindow());
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

