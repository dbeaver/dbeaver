
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UDTMethodType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UDTMethodType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CLRFunction" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CLRFunctionType" minOccurs="0"/>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UDTMethodType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "clrFunction",
    "scalarOperator"
})
public class UDTMethodType_sql2012 {

    @XmlElement(name = "CLRFunction", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CLRFunctionType_sql2012 clrFunction;
    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ScalarType_sql2012> scalarOperator;

    /**
     * Gets the value of the clrFunction property.
     * 
     * @return
     *     possible object is
     *     {@link CLRFunctionType_sql2012 }
     *     
     */
    public CLRFunctionType_sql2012 getCLRFunction() {
        return clrFunction;
    }

    /**
     * Sets the value of the clrFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link CLRFunctionType_sql2012 }
     *     
     */
    public void setCLRFunction(CLRFunctionType_sql2012 value) {
        this.clrFunction = value;
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
     * {@link ScalarType_sql2012 }
     * 
     * 
     */
    public List<ScalarType_sql2012> getScalarOperator() {
        if (scalarOperator == null) {
            scalarOperator = new ArrayList<ScalarType_sql2012>();
        }
        return this.scalarOperator;
    }

}
