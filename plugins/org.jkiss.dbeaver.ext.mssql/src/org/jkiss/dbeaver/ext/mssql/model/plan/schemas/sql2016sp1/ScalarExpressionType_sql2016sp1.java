
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ScalarExpressionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScalarExpressionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScalarExpressionType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator"
})
@XmlSeeAlso({
    SetPredicateElementType_sql2016sp1 .class
})
public class ScalarExpressionType_sql2016sp1 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ScalarType_sql2016sp1 scalarOperator;

    /**
     * Gets the value of the scalarOperator property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarType_sql2016sp1 }
     *     
     */
    public ScalarType_sql2016sp1 getScalarOperator() {
        return scalarOperator;
    }

    /**
     * Sets the value of the scalarOperator property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarType_sql2016sp1 }
     *     
     */
    public void setScalarOperator(ScalarType_sql2016sp1 value) {
        this.scalarOperator = value;
    }

}
