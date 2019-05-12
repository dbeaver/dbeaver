
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CompareOpType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CompareOpType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="BINARY IS"/>
 *     &lt;enumeration value="BOTH NULL"/>
 *     &lt;enumeration value="EQ"/>
 *     &lt;enumeration value="GE"/>
 *     &lt;enumeration value="GT"/>
 *     &lt;enumeration value="IS"/>
 *     &lt;enumeration value="IS NOT"/>
 *     &lt;enumeration value="IS NOT NULL"/>
 *     &lt;enumeration value="IS NULL"/>
 *     &lt;enumeration value="LE"/>
 *     &lt;enumeration value="LT"/>
 *     &lt;enumeration value="NE"/>
 *     &lt;enumeration value="ONE NULL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CompareOpType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum CompareOpType_sql2012 {

    @XmlEnumValue("BINARY IS")
    BINARY_IS("BINARY IS"),
    @XmlEnumValue("BOTH NULL")
    BOTH_NULL("BOTH NULL"),
    EQ("EQ"),
    GE("GE"),
    GT("GT"),
    IS("IS"),
    @XmlEnumValue("IS NOT")
    IS_NOT("IS NOT"),
    @XmlEnumValue("IS NOT NULL")
    IS_NOT_NULL("IS NOT NULL"),
    @XmlEnumValue("IS NULL")
    IS_NULL("IS NULL"),
    LE("LE"),
    LT("LT"),
    NE("NE"),
    @XmlEnumValue("ONE NULL")
    ONE_NULL("ONE NULL");
    private final String value;

    CompareOpType_sql2012(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CompareOpType_sql2012 fromValue(String v) {
        for (CompareOpType_sql2012 c: CompareOpType_sql2012 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
