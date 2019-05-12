
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SimpleUpdateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SimpleUpdateType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RowsetType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="SeekPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateType" minOccurs="0"/>
 *           &lt;element name="SeekPredicateNew" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateNewType" minOccurs="0"/>
 *         &lt;/choice>
 *         &lt;element name="SetPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="DMLRequestSort" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleUpdateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicate",
    "seekPredicateNew",
    "setPredicate"
})
public class SimpleUpdateType_sql2014
    extends RowsetType_sql2014
{

    @XmlElement(name = "SeekPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicateType_sql2014 seekPredicate;
    @XmlElement(name = "SeekPredicateNew", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicateNewType_sql2014 seekPredicateNew;
    @XmlElement(name = "SetPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2014 setPredicate;
    @XmlAttribute(name = "DMLRequestSort")
    protected Boolean dmlRequestSort;

    /**
     * Gets the value of the seekPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicateType_sql2014 }
     *     
     */
    public SeekPredicateType_sql2014 getSeekPredicate() {
        return seekPredicate;
    }

    /**
     * Sets the value of the seekPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicateType_sql2014 }
     *     
     */
    public void setSeekPredicate(SeekPredicateType_sql2014 value) {
        this.seekPredicate = value;
    }

    /**
     * Gets the value of the seekPredicateNew property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicateNewType_sql2014 }
     *     
     */
    public SeekPredicateNewType_sql2014 getSeekPredicateNew() {
        return seekPredicateNew;
    }

    /**
     * Sets the value of the seekPredicateNew property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicateNewType_sql2014 }
     *     
     */
    public void setSeekPredicateNew(SeekPredicateNewType_sql2014 value) {
        this.seekPredicateNew = value;
    }

    /**
     * Gets the value of the setPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2014 }
     *     
     */
    public ScalarExpressionType_sql2014 getSetPredicate() {
        return setPredicate;
    }

    /**
     * Sets the value of the setPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2014 }
     *     
     */
    public void setSetPredicate(ScalarExpressionType_sql2014 value) {
        this.setPredicate = value;
    }

    /**
     * Gets the value of the dmlRequestSort property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getDMLRequestSort() {
        return dmlRequestSort;
    }

    /**
     * Sets the value of the dmlRequestSort property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDMLRequestSort(Boolean value) {
        this.dmlRequestSort = value;
    }

}
