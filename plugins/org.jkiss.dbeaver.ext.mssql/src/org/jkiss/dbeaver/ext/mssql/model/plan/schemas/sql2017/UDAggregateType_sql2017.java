
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="unbounded" minOccurs="0"/>
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
public class UDAggregateType_sql2017 {

    @XmlElement(name = "UDAggObject", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ObjectType_sql2017 udAggObject;
    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ScalarType_sql2017> scalarOperator;
    @XmlAttribute(name = "Distinct", required = true)
    protected boolean distinct;

    /**
     * Gets the value of the udAggObject property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectType_sql2017 }
     *     
     */
    public ObjectType_sql2017 getUDAggObject() {
        return udAggObject;
    }

    /**
     * Sets the value of the udAggObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectType_sql2017 }
     *     
     */
    public void setUDAggObject(ObjectType_sql2017 value) {
        this.udAggObject = value;
    }

    /**
     * Gets the value of the scalarOperator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the scalarOperator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getScalarOperator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ScalarType_sql2017 }
     * 
     * 
     */
    public List<ScalarType_sql2017> getScalarOperator() {
        if (scalarOperator == null) {
            scalarOperator = new ArrayList<ScalarType_sql2017>();
        }
        return this.scalarOperator;
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
