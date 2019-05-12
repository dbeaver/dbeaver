
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
public class SimpleUpdateType_sql2008
    extends RowsetType_sql2008
{

    @XmlElement(name = "SeekPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicateType_sql2008 seekPredicate;
    @XmlElement(name = "SeekPredicateNew", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicateNewType_sql2008 seekPredicateNew;
    @XmlElement(name = "SetPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2008 setPredicate;

    /**
     * Gets the value of the seekPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicateType_sql2008 }
     *     
     */
    public SeekPredicateType_sql2008 getSeekPredicate() {
        return seekPredicate;
    }

    /**
     * Sets the value of the seekPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicateType_sql2008 }
     *     
     */
    public void setSeekPredicate(SeekPredicateType_sql2008 value) {
        this.seekPredicate = value;
    }

    /**
     * Gets the value of the seekPredicateNew property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicateNewType_sql2008 }
     *     
     */
    public SeekPredicateNewType_sql2008 getSeekPredicateNew() {
        return seekPredicateNew;
    }

    /**
     * Sets the value of the seekPredicateNew property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicateNewType_sql2008 }
     *     
     */
    public void setSeekPredicateNew(SeekPredicateNewType_sql2008 value) {
        this.seekPredicateNew = value;
    }

    /**
     * Gets the value of the setPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public ScalarExpressionType_sql2008 getSetPredicate() {
        return setPredicate;
    }

    /**
     * Sets the value of the setPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2008 }
     *     
     */
    public void setSetPredicate(ScalarExpressionType_sql2008 value) {
        this.setPredicate = value;
    }

}
