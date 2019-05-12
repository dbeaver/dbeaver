
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Provide memory grant estimate as well as actual runtime memory grant information.
 * 				Serial required/desired memory attributes are estimated during query compile time for serial execution.
 * 				The rest of attributes provide estimates and counters for query execution time considering actual degree of parallelism.
 * 				SerialRequiredMemory: Required memory in KB if the query runs in serial mode. The query will not start without this memory.
 * 				SerialDesiredMemory: Memory estimated to fit intermediate results in KB if the query runs in serial mode.
 * 				RequiredMemory: Required memory in KB for the chosen degree of parallelism. If the query runs in serial mode, this is the same as SerialRequiredMemory.
 * 				DesiredMemory: Memory estimated to fit intermediate results in KB for the chosen degree of parallelism.  If the query runs in serial mode, this is the same as SerialDesiredMemory.
 * 				RequestedMemory: Memory in KB which the query requests the memory manager to grant. This can be smaller than sum of RequiredMemory and DesiredMemory if it exceeds the maximum allowed for single query.
 * 				GrantWaitTime: Time in seconds if the query has to wait for successful memory grant.
 * 				MaxUsedMemory: Maximum memory in KB used by the query.
 * 				MaxQueryMemory: Maximum memory in KB allowed for single query.
 * 			
 * 
 * <p>Java class for MemoryGrantType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MemoryGrantType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="SerialRequiredMemory" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="SerialDesiredMemory" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="RequiredMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="DesiredMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="RequestedMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="GrantWaitTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="GrantedMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="MaxUsedMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="MaxQueryMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MemoryGrantType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class MemoryGrantType_sql2017 {

    @XmlAttribute(name = "SerialRequiredMemory", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger serialRequiredMemory;
    @XmlAttribute(name = "SerialDesiredMemory", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger serialDesiredMemory;
    @XmlAttribute(name = "RequiredMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger requiredMemory;
    @XmlAttribute(name = "DesiredMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger desiredMemory;
    @XmlAttribute(name = "RequestedMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger requestedMemory;
    @XmlAttribute(name = "GrantWaitTime")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger grantWaitTime;
    @XmlAttribute(name = "GrantedMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger grantedMemory;
    @XmlAttribute(name = "MaxUsedMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger maxUsedMemory;
    @XmlAttribute(name = "MaxQueryMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger maxQueryMemory;

    /**
     * Gets the value of the serialRequiredMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSerialRequiredMemory() {
        return serialRequiredMemory;
    }

    /**
     * Sets the value of the serialRequiredMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSerialRequiredMemory(BigInteger value) {
        this.serialRequiredMemory = value;
    }

    /**
     * Gets the value of the serialDesiredMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSerialDesiredMemory() {
        return serialDesiredMemory;
    }

    /**
     * Sets the value of the serialDesiredMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSerialDesiredMemory(BigInteger value) {
        this.serialDesiredMemory = value;
    }

    /**
     * Gets the value of the requiredMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRequiredMemory() {
        return requiredMemory;
    }

    /**
     * Sets the value of the requiredMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRequiredMemory(BigInteger value) {
        this.requiredMemory = value;
    }

    /**
     * Gets the value of the desiredMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getDesiredMemory() {
        return desiredMemory;
    }

    /**
     * Sets the value of the desiredMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setDesiredMemory(BigInteger value) {
        this.desiredMemory = value;
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
     * Gets the value of the grantWaitTime property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getGrantWaitTime() {
        return grantWaitTime;
    }

    /**
     * Sets the value of the grantWaitTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setGrantWaitTime(BigInteger value) {
        this.grantWaitTime = value;
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

    /**
     * Gets the value of the maxQueryMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxQueryMemory() {
        return maxQueryMemory;
    }

    /**
     * Sets the value of the maxQueryMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxQueryMemory(BigInteger value) {
        this.maxQueryMemory = value;
    }

}
