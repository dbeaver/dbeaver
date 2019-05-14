
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArithmeticOperationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ArithmeticOperationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ADD"/>
 *     &lt;enumeration value="BIT_ADD"/>
 *     &lt;enumeration value="BIT_AND"/>
 *     &lt;enumeration value="BIT_COMBINE"/>
 *     &lt;enumeration value="BIT_NOT"/>
 *     &lt;enumeration value="BIT_OR"/>
 *     &lt;enumeration value="BIT_XOR"/>
 *     &lt;enumeration value="DIV"/>
 *     &lt;enumeration value="HASH"/>
 *     &lt;enumeration value="MINUS"/>
 *     &lt;enumeration value="MOD"/>
 *     &lt;enumeration value="MULT"/>
 *     &lt;enumeration value="SUB"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ArithmeticOperationType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum ArithmeticOperationType_sql2017 {

    ADD,
    BIT_ADD,
    BIT_AND,
    BIT_COMBINE,
    BIT_NOT,
    BIT_OR,
    BIT_XOR,
    DIV,
    HASH,
    MINUS,
    MOD,
    MULT,
    SUB;

    public String value() {
        return name();
    }

    public static ArithmeticOperationType_sql2017 fromValue(String v) {
        return valueOf(v);
    }

}
