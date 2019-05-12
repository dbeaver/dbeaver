
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SeekPredicateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SeekPredicateType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Prefix" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScanRangeType" minOccurs="0"/>
 *         &lt;element name="StartRange" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScanRangeType" minOccurs="0"/>
 *         &lt;element name="EndRange" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScanRangeType" minOccurs="0"/>
 *         &lt;element name="IsNotNull" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SeekPredicateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "prefix",
    "startRange",
    "endRange",
    "isNotNull"
})
public class SeekPredicateType_sql2012 {

    @XmlElement(name = "Prefix", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScanRangeType_sql2012 prefix;
    @XmlElement(name = "StartRange", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScanRangeType_sql2012 startRange;
    @XmlElement(name = "EndRange", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScanRangeType_sql2012 endRange;
    @XmlElement(name = "IsNotNull", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2012 isNotNull;

    /**
     * Gets the value of the prefix property.
     * 
     * @return
     *     possible object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public ScanRangeType_sql2012 getPrefix() {
        return prefix;
    }

    /**
     * Sets the value of the prefix property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public void setPrefix(ScanRangeType_sql2012 value) {
        this.prefix = value;
    }

    /**
     * Gets the value of the startRange property.
     * 
     * @return
     *     possible object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public ScanRangeType_sql2012 getStartRange() {
        return startRange;
    }

    /**
     * Sets the value of the startRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public void setStartRange(ScanRangeType_sql2012 value) {
        this.startRange = value;
    }

    /**
     * Gets the value of the endRange property.
     * 
     * @return
     *     possible object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public ScanRangeType_sql2012 getEndRange() {
        return endRange;
    }

    /**
     * Sets the value of the endRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScanRangeType_sql2012 }
     *     
     */
    public void setEndRange(ScanRangeType_sql2012 value) {
        this.endRange = value;
    }

    /**
     * Gets the value of the isNotNull property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2012 }
     *     
     */
    public SingleColumnReferenceType_sql2012 getIsNotNull() {
        return isNotNull;
    }

    /**
     * Sets the value of the isNotNull property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2012 }
     *     
     */
    public void setIsNotNull(SingleColumnReferenceType_sql2012 value) {
        this.isNotNull = value;
    }

}
