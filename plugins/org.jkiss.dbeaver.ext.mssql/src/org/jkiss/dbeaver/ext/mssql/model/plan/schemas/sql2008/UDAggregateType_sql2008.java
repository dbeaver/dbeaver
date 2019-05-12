
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UDAggregateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UDAggregateType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="UDAggObject" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Distinct" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UDAggregateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "udAggObject",
    "scalarOperator"
})
public class UDAggregateType_sql2008 {

    @XmlElement(name = "UDAggObject", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ObjectType_sql2008 udAggObject;
    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ScalarType_sql2008 scalarOperator;
    @XmlAttribute(name = "Distinct", required = true)
    protected boolean distinct;

    /**
     * Gets the value of the udAggObject property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectType_sql2008 }
     *     
     */
    public ObjectType_sql2008 getUDAggObject() {
        return udAggObject;
    }

    /**
     * Sets the value of the udAggObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectType_sql2008 }
     *     
     */
    public void setUDAggObject(ObjectType_sql2008 value) {
        this.udAggObject = value;
    }

    /**
     * Gets the value of the scalarOperator property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarType_sql2008 }
     *     
     */
    public ScalarType_sql2008 getScalarOperator() {
        return scalarOperator;
    }

    /**
     * Sets the value of the scalarOperator property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarType_sql2008 }
     *     
     */
    public void setScalarOperator(ScalarType_sql2008 value) {
        this.scalarOperator = value;
    }

    /**
     * Gets the value of the distinct property.
     * 
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets the value of the distinct property.
     * 
     */
    public void setDistinct(boolean value) {
        this.distinct = value;
    }

}
