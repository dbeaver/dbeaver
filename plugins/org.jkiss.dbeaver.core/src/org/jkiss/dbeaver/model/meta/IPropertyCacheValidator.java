package org.jkiss.dbeaver.model.meta;

/**
 * Property cache validator
 */
public interface IPropertyCacheValidator<OBJECT_TYPE> {

    boolean isPropertyCached(OBJECT_TYPE object);

}
