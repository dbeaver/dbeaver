package org.jkiss.dbeaver.ext.dm.sql;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;

public class DmDialectAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] CLASSES = new Class[] { TPRuleProvider.class };

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return CLASSES;
	}

}
