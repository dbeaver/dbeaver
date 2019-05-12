
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArithmeticType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArithmeticType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="2"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Operation" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ArithmeticOperationType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArithmeticType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator"
})
public class ArithmeticType_sql2005sp2 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ScalarType_sql2005sp2> scalarOperator;
    @XmlAttribute(name = "Operation", required = true)
    protected ArithmeticOperationType_sql2005sp2 operation;

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
     * {@link ScalarType_sql2005sp2 }
     * 
     * 
     */
    public List<ScalarType_sql2005sp2> getScalarOperator() {
        if (scalarOperator == null) {
            scalarOperator = new ArrayList<ScalarType_sql2005sp2>();
        }
        return this.scalarOperator;
    }

    /**
     * Gets the value of the operation property.
     * 
     * @return
     *     possible object is
     *     {@link ArithmeticOperationType_sql2005sp2 }
     *     
     */
    public ArithmeticOperationType_sql2005sp2 getOperation() {
        return operation;
    }

    /**
     * Sets the value of the operation property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArithmeticOperationType_sql2005sp2 }
     *     
     */
    public void setOperation(ArithmeticOperationType_sql2005sp2 value) {
        this.operation = value;
    }

}
