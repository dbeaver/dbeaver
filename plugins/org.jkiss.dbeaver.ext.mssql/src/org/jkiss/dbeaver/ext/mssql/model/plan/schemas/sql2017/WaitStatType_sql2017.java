
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Wait statistics during one query execution.
 * 					WaitType: Name of the wait
 * 					WaitTimeMs: Wait time in milliseconds
 * 					WaitCount: Number of waits
 * 			
 * 
 * <p>Java class for WaitStatType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WaitStatType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="WaitType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="WaitTimeMs" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="WaitCount" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WaitStatType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class WaitStatType_sql2017 {

    @XmlAttribute(name = "WaitType", required = true)
    protected String waitType;
    @XmlAttribute(name = "WaitTimeMs", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger waitTimeMs;
    @XmlAttribute(name = "WaitCount", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger waitCount;

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
     * Gets the value of the waitTimeMs property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getWaitTimeMs() {
        return waitTimeMs;
    }

    /**
     * Sets the value of the waitTimeMs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setWaitTimeMs(BigInteger value) {
        this.waitTimeMs = value;
    }

    /**
     * Gets the value of the waitCount property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getWaitCount() {
        return waitCount;
    }

    /**
     * Sets the value of the waitCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setWaitCount(BigInteger value) {
        this.waitCount = value;
    }

}
