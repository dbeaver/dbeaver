/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Catalog is a simple entity container.
 * Do not provides any additional attributes but may be used in some JDBC specific issues
 * to determine difference in catalog/schema containment.
 */
public interface DBSCatalog extends DBSObject, DBSObjectContainer
{

}
