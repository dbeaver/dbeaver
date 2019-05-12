
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UDFType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UDFType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="CLRFunction" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CLRFunctionType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="FunctionName" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="IsClrFunction" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UDFType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator",
    "clrFunction"
})
public class UDFType_sql2016sp1 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ScalarType_sql2016sp1> scalarOperator;
    @XmlElement(name = "CLRFunction", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected CLRFunctionType_sql2016sp1 clrFunction;
    @XmlAttribute(name = "FunctionName", required = true)
    protected String functionName;
    @XmlAttribute(name = "IsClrFunction")
    protected Boolean isClrFunction;

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
     * {@link ScalarType_sql2016sp1 }
     * 
     * 
     */
    public List<ScalarType_sql2016sp1> getScalarOperator() {
        if (scalarOperator == null) {
            scalarOperator = new ArrayList<ScalarType_sql2016sp1>();
        }
        return this.scalarOperator;
    }

    /**
     * Gets the value of the clrFunction property.
     * 
     * @return
     *     possible object is
     *     {@link CLRFunctionType_sql2016sp1 }
     *     
     */
    public CLRFunctionType_sql2016sp1 getCLRFunction() {
        return clrFunction;
    }

    /**
     * Sets the value of the clrFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link CLRFunctionType_sql2016sp1 }
     *     
     */
    public void setCLRFunction(CLRFunctionType_sql2016sp1 value) {
        this.clrFunction = value;
    }

    /**
     * Gets the value of the functionName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Sets the value of the functionName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFunctionName(String value) {
        this.functionName = value;
    }

    /**
     * Gets the value of the isClrFunction property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getIsClrFunction() {
        return isClrFunction;
    }

    /**
     * Sets the value of the isClrFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsClrFunction(Boolean value) {
        this.isClrFunction = value;
    }

}
