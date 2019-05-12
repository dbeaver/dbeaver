
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ConstType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConstType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="ConstValue" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConstType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class ConstType_sql2014 {

    @XmlAttribute(name = "ConstValue", required = true)
    protected String constValue;

    /**
     * Gets the value of the constValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConstValue() {
        return constValue;
    }

    /**
     * Sets the value of the constValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConstValue(String value) {
        this.constValue = value;
    }

}
