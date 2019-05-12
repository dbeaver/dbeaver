
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SubqueryOperationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SubqueryOperationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="EQ ALL"/>
 *     &lt;enumeration value="EQ ANY"/>
 *     &lt;enumeration value="EXISTS"/>
 *     &lt;enumeration value="GE ALL"/>
 *     &lt;enumeration value="GE ANY"/>
 *     &lt;enumeration value="GT ALL"/>
 *     &lt;enumeration value="GT ANY"/>
 *     &lt;enumeration value="IN"/>
 *     &lt;enumeration value="LE ALL"/>
 *     &lt;enumeration value="LE ANY"/>
 *     &lt;enumeration value="LT ALL"/>
 *     &lt;enumeration value="LT ANY"/>
 *     &lt;enumeration value="NE ALL"/>
 *     &lt;enumeration value="NE ANY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SubqueryOperationType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum SubqueryOperationType_sql2014 {

    @XmlEnumValue("EQ ALL")
    EQ_ALL("EQ ALL"),
    @XmlEnumValue("EQ ANY")
    EQ_ANY("EQ ANY"),
    EXISTS("EXISTS"),
    @XmlEnumValue("GE ALL")
    GE_ALL("GE ALL"),
    @XmlEnumValue("GE ANY")
    GE_ANY("GE ANY"),
    @XmlEnumValue("GT ALL")
    GT_ALL("GT ALL"),
    @XmlEnumValue("GT ANY")
    GT_ANY("GT ANY"),
    IN("IN"),
    @XmlEnumValue("LE ALL")
    LE_ALL("LE ALL"),
    @XmlEnumValue("LE ANY")
    LE_ANY("LE ANY"),
    @XmlEnumValue("LT ALL")
    LT_ALL("LT ALL"),
    @XmlEnumValue("LT ANY")
    LT_ANY("LT ANY"),
    @XmlEnumValue("NE ALL")
    NE_ALL("NE ALL"),
    @XmlEnumValue("NE ANY")
    NE_ANY("NE ANY");
    private final String value;

    SubqueryOperationType_sql2014(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SubqueryOperationType_sql2014 fromValue(String v) {
        for (SubqueryOperationType_sql2014 c: SubqueryOperationType_sql2014 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
