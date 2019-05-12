
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AggregateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AggregateType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="AggType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Distinct" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AggregateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator"
})
public class AggregateType_sql2005 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ScalarType_sql2005> scalarOperator;
    @XmlAttribute(name = "AggType", required = true)
    protected String aggType;
    @XmlAttribute(name = "Distinct", required = true)
    protected boolean distinct;

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
     * {@link ScalarType_sql2005 }
     * 
     * 
     */
    public List<ScalarType_sql2005> getScalarOperator() {
        if (scalarOperator == null) {
            scalarOperator = new ArrayList<ScalarType_sql2005>();
        }
        return this.scalarOperator;
    }

    /**
     * Gets the value of the aggType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAggType() {
        return aggType;
    }

    /**
     * Sets the value of the aggType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAggType(String value) {
        this.aggType = value;
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
