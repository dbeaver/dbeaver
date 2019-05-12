
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OrderType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="OrderType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="BACKWARD"/>
 *     &lt;enumeration value="FORWARD"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "OrderType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum OrderType_sql2014 {

    BACKWARD,
    FORWARD;

    public String value() {
        return name();
    }

    public static OrderType_sql2014 fromValue(String v) {
        return valueOf(v);
    }

}
