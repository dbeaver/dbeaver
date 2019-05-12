
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TraceFlagScopeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="TraceFlagScopeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Global"/>
 *     &lt;enumeration value="Session"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "TraceFlagScopeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum TraceFlagScopeType_sql2017 {

    @XmlEnumValue("Global")
    GLOBAL("Global"),
    @XmlEnumValue("Session")
    SESSION("Session");
    private final String value;

    TraceFlagScopeType_sql2017(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TraceFlagScopeType_sql2017 fromValue(String v) {
        for (TraceFlagScopeType_sql2017 c: TraceFlagScopeType_sql2017 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
