
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Provide warning information for memory grant.
 * 				GrantWarningKind: Warning kind
 * 				RequestedMemory: Initial grant request in KB
 * 				GrantedMemory: Granted memory in KB
 * 				MaxUsedMemory: Maximum used memory grant in KB
 * 			
 * 
 * <p>Java class for MemoryGrantWarningInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MemoryGrantWarningInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="GrantWarningKind" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MemoryGrantWarningType" />
 *       &lt;attribute name="RequestedMemory" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="GrantedMemory" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="MaxUsedMemory" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MemoryGrantWarningInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class MemoryGrantWarningInfo_sql2016sp1 {

    @XmlAttribute(name = "GrantWarningKind", required = true)
    protected MemoryGrantWarningType_sql2016sp1 grantWarningKind;
    @XmlAttribute(name = "RequestedMemory", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger requestedMemory;
    @XmlAttribute(name = "GrantedMemory", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger grantedMemory;
    @XmlAttribute(name = "MaxUsedMemory", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger maxUsedMemory;

    /**
     * Gets the value of the grantWarningKind property.
     * 
     * @return
     *     possible object is
     *     {@link MemoryGrantWarningType_sql2016sp1 }
     *     
     */
    public MemoryGrantWarningType_sql2016sp1 getGrantWarningKind() {
        return grantWarningKind;
    }

    /**
     * Sets the value of the grantWarningKind property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryGrantWarningType_sql2016sp1 }
     *     
     */
    public void setGrantWarningKind(MemoryGrantWarningType_sql2016sp1 value) {
        this.grantWarningKind = value;
    }

    /**
     * Gets the value of the requestedMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRequestedMemory() {
        return requestedMemory;
    }

    /**
     * Sets the value of the requestedMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRequestedMemory(BigInteger value) {
        this.requestedMemory = value;
    }

    /**
     * Gets the value of the grantedMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getGrantedMemory() {
        return grantedMemory;
    }

    /**
     * Sets the value of the grantedMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setGrantedMemory(BigInteger value) {
        this.grantedMemory = value;
    }

    /**
     * Gets the value of the maxUsedMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxUsedMemory() {
        return maxUsedMemory;
    }

    /**
     * Sets the value of the maxUsedMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxUsedMemory(BigInteger value) {
        this.maxUsedMemory = value;
    }

}
