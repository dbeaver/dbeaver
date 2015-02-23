/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import java.util.*;

public final class PropertyEventDispatcher {
	private final Map fHandlerMap= new HashMap();
	private final Map fReverseMap= new HashMap();
	private final IPreferenceStore fStore;
	private final IPropertyChangeListener fListener= new IPropertyChangeListener() {
		@Override
        public void propertyChange(PropertyChangeEvent event) {
			firePropertyChange(event);
		}
	};
	public PropertyEventDispatcher(IPreferenceStore store) {
		Assert.isLegal(store != null);
		fStore= store;
	}
	public void dispose() {
		if (!fReverseMap.isEmpty())
			fStore.removePropertyChangeListener(fListener);
		fReverseMap.clear();
		fHandlerMap.clear();
	}
	private void firePropertyChange(PropertyChangeEvent event) {
		Object value= fHandlerMap.get(event.getProperty());
		if (value instanceof IPropertyChangeListener)
			((IPropertyChangeListener) value).propertyChange(event);
		else if (value instanceof Set)
			for (Iterator it= ((Set) value).iterator(); it.hasNext(); )
				((IPropertyChangeListener) it.next()).propertyChange(event);
	}
	public void addPropertyChangeListener(String property, IPropertyChangeListener listener) {
		Assert.isLegal(property != null);
		Assert.isLegal(listener != null);

		if (fReverseMap.isEmpty())
			fStore.addPropertyChangeListener(fListener);

		multiMapPut(fHandlerMap, property, listener);
		multiMapPut(fReverseMap, listener, property);
	}
	private void multiMapPut(Map map, Object key, Object value) {
		Object mapping= map.get(key);
		if (mapping == null) {
			map.put(key, value);
		} else if (mapping instanceof Set) {
			((Set) mapping).add(value);
		} else {
			Set set= new LinkedHashSet();
			set.add(mapping);
			set.add(value);
			map.put(key, set);
		}
	}
	private void multiMapRemove(Map map, Object key, Object value) {
		Object mapping= map.get(key);
		if (mapping instanceof Set) {
			((Set) mapping).remove(value);
		} else if (mapping != null) {
			map.remove(key);
		}
	}
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		Object value= fReverseMap.get(listener);
		if (value == null)
			return;
		if (value instanceof String) {
			fReverseMap.remove(listener);
			multiMapRemove(fHandlerMap, value, listener);
		} else if (value instanceof Set) {
			fReverseMap.remove(listener);
			for (Iterator it= ((Set) value).iterator(); it.hasNext();)
				multiMapRemove(fHandlerMap, it.next(), listener);
		}

		if (fReverseMap.isEmpty())
			fStore.removePropertyChangeListener(fListener);
	}
}