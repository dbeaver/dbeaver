
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CloneAccessScopeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CloneAccessScopeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Primary"/>
 *     &lt;enumeration value="Secondary"/>
 *     &lt;enumeration value="Both"/>
 *     &lt;enumeration value="Either"/>
 *     &lt;enumeration value="ExactMatch"/>
 *     &lt;enumeration value="Local"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CloneAccessScopeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum CloneAccessScopeType_sql2012 {

    @XmlEnumValue("Primary")
    PRIMARY("Primary"),
    @XmlEnumValue("Secondary")
    SECONDARY("Secondary"),
    @XmlEnumValue("Both")
    BOTH("Both"),
    @XmlEnumValue("Either")
    EITHER("Either"),
    @XmlEnumValue("ExactMatch")
    EXACT_MATCH("ExactMatch"),
    @XmlEnumValue("Local")
    LOCAL("Local");
    private final String value;

    CloneAccessScopeType_sql2012(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CloneAccessScopeType_sql2012 fromValue(String v) {
        for (CloneAccessScopeType_sql2012 c: CloneAccessScopeType_sql2012 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
