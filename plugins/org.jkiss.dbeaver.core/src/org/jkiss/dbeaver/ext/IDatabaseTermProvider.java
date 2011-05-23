/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

/**
 * Term provider.
 */
public interface IDatabaseTermProvider {

    /**
     * Underlying datasource
     * @return data source object.
     */
    String getObjectTypeTerm(String path, boolean multiple);

}
