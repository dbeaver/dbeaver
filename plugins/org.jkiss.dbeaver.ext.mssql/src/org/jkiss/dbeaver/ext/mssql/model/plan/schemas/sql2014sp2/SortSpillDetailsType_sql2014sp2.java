
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Sort spill details
 * 
 * <p>Java class for SortSpillDetailsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SortSpillDetailsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="GrantedMemoryKb" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="UsedMemoryKb" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="WritesToTempDb" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="ReadsFromTempDb" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SortSpillDetailsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class SortSpillDetailsType_sql2014sp2 {

    @XmlAttribute(name = "GrantedMemoryKb")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger grantedMemoryKb;
    @XmlAttribute(name = "UsedMemoryKb")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger usedMemoryKb;
    @XmlAttribute(name = "WritesToTempDb")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger writesToTempDb;
    @XmlAttribute(name = "ReadsFromTempDb")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger readsFromTempDb;

    /**
     * Gets the value of the grantedMemoryKb property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getGrantedMemoryKb() {
        return grantedMemoryKb;
    }

    /**
     * Sets the value of the grantedMemoryKb property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setGrantedMemoryKb(BigInteger value) {
        this.grantedMemoryKb = value;
    }

    /**
     * Gets the value of the usedMemoryKb property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getUsedMemoryKb() {
        return usedMemoryKb;
    }

    /**
     * Sets the value of the usedMemoryKb property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setUsedMemoryKb(BigInteger value) {
        this.usedMemoryKb = value;
    }

    /**
     * Gets the value of the writesToTempDb property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getWritesToTempDb() {
        return writesToTempDb;
    }

    /**
     * Sets the value of the writesToTempDb property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setWritesToTempDb(BigInteger value) {
        this.writesToTempDb = value;
    }

    /**
     * Gets the value of the readsFromTempDb property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getReadsFromTempDb() {
        return readsFromTempDb;
    }

    /**
     * Sets the value of the readsFromTempDb property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setReadsFromTempDb(BigInteger value) {
        this.readsFromTempDb = value;
    }

}
