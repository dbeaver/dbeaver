
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ExecutionModeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ExecutionModeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Row"/>
 *     &lt;enumeration value="Batch"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ExecutionModeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum ExecutionModeType_sql2014 {

    @XmlEnumValue("Row")
    ROW("Row"),
    @XmlEnumValue("Batch")
    BATCH("Batch");
    private final String value;

    ExecutionModeType_sql2014(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ExecutionModeType_sql2014 fromValue(String v) {
        for (ExecutionModeType_sql2014 c: ExecutionModeType_sql2014 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
