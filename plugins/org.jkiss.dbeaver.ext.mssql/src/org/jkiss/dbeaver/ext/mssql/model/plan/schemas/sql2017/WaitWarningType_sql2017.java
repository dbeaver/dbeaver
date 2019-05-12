
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Query wait information
 * 
 * <p>Java class for WaitWarningType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WaitWarningType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="WaitType" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Memory Grant"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="WaitTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WaitWarningType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class WaitWarningType_sql2017 {

    @XmlAttribute(name = "WaitType", required = true)
    protected String waitType;
    @XmlAttribute(name = "WaitTime")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger waitTime;

    /**
     * Gets the value of the waitType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWaitType() {
        return waitType;
    }

    /**
     * Sets the value of the waitType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWaitType(String value) {
        this.waitType = value;
    }

    /**
     * Gets the value of the waitTime property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getWaitTime() {
        return waitTime;
    }

    /**
     * Sets the value of the waitTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setWaitTime(BigInteger value) {
        this.waitTime = value;
    }

}
