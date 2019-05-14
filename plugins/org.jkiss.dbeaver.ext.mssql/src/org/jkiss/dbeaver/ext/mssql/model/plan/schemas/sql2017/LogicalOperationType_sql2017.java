
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LogicalOperationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LogicalOperationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="AND"/>
 *     &lt;enumeration value="IMPLIES"/>
 *     &lt;enumeration value="IS NOT NULL"/>
 *     &lt;enumeration value="IS NULL"/>
 *     &lt;enumeration value="IS"/>
 *     &lt;enumeration value="IsFalseOrNull"/>
 *     &lt;enumeration value="NOT"/>
 *     &lt;enumeration value="OR"/>
 *     &lt;enumeration value="XOR"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LogicalOperationType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum LogicalOperationType_sql2017 {

    AND("AND"),
    IMPLIES("IMPLIES"),
    @XmlEnumValue("IS NOT NULL")
    IS_NOT_NULL("IS NOT NULL"),
    @XmlEnumValue("IS NULL")
    IS_NULL("IS NULL"),
    IS("IS"),
    @XmlEnumValue("IsFalseOrNull")
    IS_FALSE_OR_NULL("IsFalseOrNull"),
    NOT("NOT"),
    OR("OR"),
    XOR("XOR");
    private final String value;

    LogicalOperationType_sql2017(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LogicalOperationType_sql2017 fromValue(String v) {
        for (LogicalOperationType_sql2017 c: LogicalOperationType_sql2017 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
