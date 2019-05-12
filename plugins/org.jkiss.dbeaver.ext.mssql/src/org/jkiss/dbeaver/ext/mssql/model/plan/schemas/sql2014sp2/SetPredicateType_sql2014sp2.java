
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SetPredicateType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SetPredicateType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Update"/>
 *     &lt;enumeration value="Insert"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SetPredicateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum SetPredicateType_sql2014sp2 {

    @XmlEnumValue("Update")
    UPDATE("Update"),
    @XmlEnumValue("Insert")
    INSERT("Insert");
    private final String value;

    SetPredicateType_sql2014sp2(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SetPredicateType_sql2014sp2 fromValue(String v) {
        for (SetPredicateType_sql2014sp2 c: SetPredicateType_sql2014sp2 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
