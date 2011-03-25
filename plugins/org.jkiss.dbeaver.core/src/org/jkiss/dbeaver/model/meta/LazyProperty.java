package org.jkiss.dbeaver.model.meta;

/**
 * Lazy property annotation
 */
public @interface LazyProperty {

    Class<? extends IPropertyCacheValidator> cacheValidator();

}
