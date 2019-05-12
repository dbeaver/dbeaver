
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *         Provide hardware-dependent properties that affect cost estimate (and hence, query plan choice), as seen by the Query Optimizer.
 *         EstimatedAvailableMemoryGrant is an estimate of what amount of memory (KB) will be available for this query at the execution time to request a memory grant from.
 *         EstimatedPagesCached is an estimate of how many pages of data will remain cached in the buffer pool if the query needs to read it again.
 *         EstimatedAvailableDegreeOfParallelism is an estimate of number of CPUs that can be used to execute the query should the Query Optimizer pick a parallel plan.
 *         MaxCompileMemory is the maximum memory in KB allowed for query optimizer to use during compilation.
 *       
 * 
 * <p>Java class for OptimizerHardwareDependentPropertiesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OptimizerHardwareDependentPropertiesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="EstimatedAvailableMemoryGrant" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="EstimatedPagesCached" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="EstimatedAvailableDegreeOfParallelism" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="MaxCompileMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OptimizerHardwareDependentPropertiesType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class OptimizerHardwareDependentPropertiesType_sql2016sp1 {

    @XmlAttribute(name = "EstimatedAvailableMemoryGrant", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger estimatedAvailableMemoryGrant;
    @XmlAttribute(name = "EstimatedPagesCached", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger estimatedPagesCached;
    @XmlAttribute(name = "EstimatedAvailableDegreeOfParallelism")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger estimatedAvailableDegreeOfParallelism;
    @XmlAttribute(name = "MaxCompileMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger maxCompileMemory;

    /**
     * Gets the value of the estimatedAvailableMemoryGrant property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getEstimatedAvailableMemoryGrant() {
        return estimatedAvailableMemoryGrant;
    }

    /**
     * Sets the value of the estimatedAvailableMemoryGrant property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setEstimatedAvailableMemoryGrant(BigInteger value) {
        this.estimatedAvailableMemoryGrant = value;
    }

    /**
     * Gets the value of the estimatedPagesCached property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getEstimatedPagesCached() {
        return estimatedPagesCached;
    }

    /**
     * Sets the value of the estimatedPagesCached property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setEstimatedPagesCached(BigInteger value) {
        this.estimatedPagesCached = value;
    }

    /**
     * Gets the value of the estimatedAvailableDegreeOfParallelism property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getEstimatedAvailableDegreeOfParallelism() {
        return estimatedAvailableDegreeOfParallelism;
    }

    /**
     * Sets the value of the estimatedAvailableDegreeOfParallelism property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setEstimatedAvailableDegreeOfParallelism(BigInteger value) {
        this.estimatedAvailableDegreeOfParallelism = value;
    }

    /**
     * Gets the value of the maxCompileMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxCompileMemory() {
        return maxCompileMemory;
    }

    /**
     * Sets the value of the maxCompileMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxCompileMemory(BigInteger value) {
        this.maxCompileMemory = value;
    }

}
