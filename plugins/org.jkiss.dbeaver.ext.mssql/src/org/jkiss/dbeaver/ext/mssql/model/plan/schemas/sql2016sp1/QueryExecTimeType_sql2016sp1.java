
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Shows time statistics for single query execution.
 * 				CpuTime: CPU time in milliseconds
 * 				ElapsedTime: elapsed time in milliseconds
 * 			
 * 
 * <p>Java class for QueryExecTimeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryExecTimeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="CpuTime" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="ElapsedTime" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryExecTimeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class QueryExecTimeType_sql2016sp1 {

    @XmlAttribute(name = "CpuTime", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger cpuTime;
    @XmlAttribute(name = "ElapsedTime", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger elapsedTime;

    /**
     * Gets the value of the cpuTime property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCpuTime() {
        return cpuTime;
    }

    /**
     * Sets the value of the cpuTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCpuTime(BigInteger value) {
        this.cpuTime = value;
    }

    /**
     * Gets the value of the elapsedTime property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Sets the value of the elapsedTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setElapsedTime(BigInteger value) {
        this.elapsedTime = value;
    }

}
