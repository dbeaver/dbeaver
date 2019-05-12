
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MemoryGrantWarningType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="MemoryGrantWarningType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Excessive Grant"/>
 *     &lt;enumeration value="Used More Than Granted"/>
 *     &lt;enumeration value="Grant Increase"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "MemoryGrantWarningType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum MemoryGrantWarningType_sql2014sp2 {

    @XmlEnumValue("Excessive Grant")
    EXCESSIVE_GRANT("Excessive Grant"),
    @XmlEnumValue("Used More Than Granted")
    USED_MORE_THAN_GRANTED("Used More Than Granted"),
    @XmlEnumValue("Grant Increase")
    GRANT_INCREASE("Grant Increase");
    private final String value;

    MemoryGrantWarningType_sql2014sp2(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MemoryGrantWarningType_sql2014sp2 fromValue(String v) {
        for (MemoryGrantWarningType_sql2014sp2 c: MemoryGrantWarningType_sql2014sp2 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
