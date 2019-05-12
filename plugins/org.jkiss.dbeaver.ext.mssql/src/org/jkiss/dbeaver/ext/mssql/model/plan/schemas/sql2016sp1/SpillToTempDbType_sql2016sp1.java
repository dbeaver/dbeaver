
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Spill warning information
 * 
 * <p>Java class for SpillToTempDbType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SpillToTempDbType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="SpillLevel" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="SpilledThreadCount" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpillToTempDbType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class SpillToTempDbType_sql2016sp1 {

    @XmlAttribute(name = "SpillLevel")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger spillLevel;
    @XmlAttribute(name = "SpilledThreadCount")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger spilledThreadCount;

    /**
     * Gets the value of the spillLevel property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSpillLevel() {
        return spillLevel;
    }

    /**
     * Sets the value of the spillLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSpillLevel(BigInteger value) {
        this.spillLevel = value;
    }

    /**
     * Gets the value of the spilledThreadCount property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSpilledThreadCount() {
        return spilledThreadCount;
    }

    /**
     * Sets the value of the spilledThreadCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSpilledThreadCount(BigInteger value) {
        this.spilledThreadCount = value;
    }

}
