package org.jkiss.dbeaver.ext.yashandb.model;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBLazyReference {
    final String schemaName;
    final String objectName;

    YashanDBLazyReference(String schemaName, String objectName) {
        this.schemaName = schemaName;
        this.objectName = objectName;
    }
}
