
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CompareType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CompareType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="2"/>
 *       &lt;/sequence>
 *       &lt;attribute name="CompareOp" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CompareOpType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompareType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator"
})
public class CompareType_sql2017 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ScalarType_sql2017> scalarOperator;
    @XmlAttribute(name = "CompareOp", required = true)
    protected CompareOpType_sql2017 compareOp;

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
     * Gets the value of the compareOp property.
     * 
     * @return
     *     possible object is
     *     {@link CompareOpType_sql2017 }
     *     
     */
    public CompareOpType_sql2017 getCompareOp() {
        return compareOp;
    }

    /**
     * Sets the value of the compareOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompareOpType_sql2017 }
     *     
     */
    public void setCompareOp(CompareOpType_sql2017 value) {
        this.compareOp = value;
    }

}
