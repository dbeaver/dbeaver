
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for StorageType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="StorageType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="RowStore"/>
 *     &lt;enumeration value="ColumnStore"/>
 *     &lt;enumeration value="MemoryOptimized"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "StorageType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum StorageType_sql2014sp2 {

    @XmlEnumValue("RowStore")
    ROW_STORE("RowStore"),
    @XmlEnumValue("ColumnStore")
    COLUMN_STORE("ColumnStore"),
    @XmlEnumValue("MemoryOptimized")
    MEMORY_OPTIMIZED("MemoryOptimized");
    private final String value;

    StorageType_sql2014sp2(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static StorageType_sql2014sp2 fromValue(String v) {
        for (StorageType_sql2014sp2 c: StorageType_sql2014sp2 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
