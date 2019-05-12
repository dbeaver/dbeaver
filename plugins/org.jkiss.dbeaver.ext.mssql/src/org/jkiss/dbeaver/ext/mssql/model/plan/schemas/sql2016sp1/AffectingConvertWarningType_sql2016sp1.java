
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Warning information for plan-affecting type conversion
 * 
 * <p>Java class for AffectingConvertWarningType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AffectingConvertWarningType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="ConvertIssue" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Cardinality Estimate"/>
 *             &lt;enumeration value="Seek Plan"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="Expression" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AffectingConvertWarningType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class AffectingConvertWarningType_sql2016sp1 {

    @XmlAttribute(name = "ConvertIssue", required = true)
    protected String convertIssue;
    @XmlAttribute(name = "Expression", required = true)
    protected String expression;

    /**
     * Gets the value of the convertIssue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConvertIssue() {
        return convertIssue;
    }

    /**
     * Sets the value of the convertIssue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConvertIssue(String value) {
        this.convertIssue = value;
    }

    /**
     * Gets the value of the expression property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the value of the expression property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExpression(String value) {
        this.expression = value;
    }

}
