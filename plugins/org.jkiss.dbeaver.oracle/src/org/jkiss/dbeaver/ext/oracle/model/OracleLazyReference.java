/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
* Lazy object reference
*/
class OracleLazyReference {
    final String schemaName;
    final String objectName;

    OracleLazyReference(String schemaName, String objectName)
    {
        this.schemaName = schemaName;
        this.objectName = objectName;
    }

}
