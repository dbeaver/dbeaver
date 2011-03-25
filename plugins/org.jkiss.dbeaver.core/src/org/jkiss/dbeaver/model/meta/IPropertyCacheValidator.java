package org.jkiss.dbeaver.model.meta;

/**
 * Property cache validator
 */
public interface IPropertyCacheValidator<OBJECT_TYPE> {

    public static final IPropertyCacheValidator<Object> NULL_VALIDATOR = new IPropertyCacheValidator<Object>() {
        public boolean isPropertyCached(Object object)
        {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    boolean isPropertyCached(OBJECT_TYPE object);

}
