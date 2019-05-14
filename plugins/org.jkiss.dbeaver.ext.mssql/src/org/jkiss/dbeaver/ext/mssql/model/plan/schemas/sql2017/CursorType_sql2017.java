
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CursorType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CursorType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Dynamic"/>
 *     &lt;enumeration value="FastForward"/>
 *     &lt;enumeration value="Keyset"/>
 *     &lt;enumeration value="SnapShot"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CursorType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum CursorType_sql2017 {

    @XmlEnumValue("Dynamic")
    DYNAMIC("Dynamic"),
    @XmlEnumValue("FastForward")
    FAST_FORWARD("FastForward"),
    @XmlEnumValue("Keyset")
    KEYSET("Keyset"),
    @XmlEnumValue("SnapShot")
    SNAP_SHOT("SnapShot");
    private final String value;

    CursorType_sql2017(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CursorType_sql2017 fromValue(String v) {
        for (CursorType_sql2017 c: CursorType_sql2017 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
