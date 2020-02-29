/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Provides base class support for model objects to participate in event handling framework
 *
 * @author Serge Rider
 */
public abstract class ERDObject<OBJECT> implements IAdaptable, DBPNamedObject {

    public static final String CHILD = "CHILD";
    public static final String REORDER = "REORDER";
    public static final String INPUT = "INPUT";
    public static final String OUTPUT = "OUTPUT";
    public static final String NAME = "NAME";

    private transient PropertyChangeSupport listeners = null;//new PropertyChangeSupport(this);

    protected OBJECT object;
    protected Object userData;

    protected ERDObject(OBJECT object) {
        this.object = object;
    }

    public OBJECT getObject() {
        return object;
    }

    public void setObject(OBJECT object) {
        this.object = object;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (listeners == null) {
            listeners = new PropertyChangeSupport(this);
        }
        listeners.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (listeners != null) {
            listeners.removePropertyChangeListener(l);
        }
    }

    public void firePropertyChange(String prop, Object old, Object newValue) {
        if (listeners != null) {
            listeners.firePropertyChange(prop, old, newValue);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBPPropertySource.class) {
            return adapter.cast(new PropertyCollector(object, false));
        }
        return null;
    }
}
