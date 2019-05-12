
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Describe a trace flag used in SQL engine.
 * 			
 * 
 * <p>Java class for TraceFlagType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TraceFlagType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Value" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="Scope" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TraceFlagScopeType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TraceFlagType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class TraceFlagType_sql2016sp1 {

    @XmlAttribute(name = "Value", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger value;
    @XmlAttribute(name = "Scope", required = true)
    protected TraceFlagScopeType_sql2016sp1 scope;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setValue(BigInteger value) {
        this.value = value;
    }

    /**
     * Gets the value of the scope property.
     * 
     * @return
     *     possible object is
     *     {@link TraceFlagScopeType_sql2016sp1 }
     *     
     */
    public TraceFlagScopeType_sql2016sp1 getScope() {
        return scope;
    }

    /**
     * Sets the value of the scope property.
     * 
     * @param value
     *     allowed object is
     *     {@link TraceFlagScopeType_sql2016sp1 }
     *     
     */
    public void setScope(TraceFlagScopeType_sql2016sp1 value) {
        this.scope = value;
    }

}
