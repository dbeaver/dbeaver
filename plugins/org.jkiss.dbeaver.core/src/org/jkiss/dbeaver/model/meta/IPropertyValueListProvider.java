package org.jkiss.dbeaver.model.meta;

import java.util.Collection;

/**
 * Property value provider
 */
public interface IPropertyValueListProvider {

    Collection<Object> getPossibleValues();

}
