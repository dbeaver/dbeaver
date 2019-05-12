
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			New Runtime information:
 * 			DegreeOfParallelism
 * 			MemoryGrant (in kilobytes)
 * 			
 * 			New compile time information:
 * 			mem fractions
 * 			CachedPlanSize (in kilobytes)
 * 			CompileTime (in milliseconds)
 * 			CompileCPU (in milliseconds)
 * 			CompileMemory (in kilobytes)
 * 			Parameter values used during query compilation
 * 			
 * 
 * <p>Java class for QueryPlanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryPlanType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="InternalInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}InternalInfoType" minOccurs="0"/>
 *         &lt;element name="MissingIndexes" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MissingIndexesType" minOccurs="0"/>
 *         &lt;element name="GuessedSelectivity" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}GuessedSelectivityType" minOccurs="0"/>
 *         &lt;element name="UnmatchedIndexes" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}UnmatchedIndexesType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *         &lt;element name="ParameterList" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="DegreeOfParallelism" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="MemoryGrant" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="CachedPlanSize" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="CompileTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="CompileCPU" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="CompileMemory" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="UsePlan" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryPlanType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "internalInfo",
    "missingIndexes",
    "guessedSelectivity",
    "unmatchedIndexes",
    "relOp",
    "parameterList"
})
public class QueryPlanType_sql2008 {

    @XmlElement(name = "InternalInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected InternalInfoType_sql2008 internalInfo;
    @XmlElement(name = "MissingIndexes", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected MissingIndexesType_sql2008 missingIndexes;
    @XmlElement(name = "GuessedSelectivity", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected GuessedSelectivityType_sql2008 guessedSelectivity;
    @XmlElement(name = "UnmatchedIndexes", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected UnmatchedIndexesType_sql2008 unmatchedIndexes;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2008 relOp;
    @XmlElement(name = "ParameterList", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 parameterList;
    @XmlAttribute(name = "DegreeOfParallelism")
    protected Integer degreeOfParallelism;
    @XmlAttribute(name = "MemoryGrant")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger memoryGrant;
    @XmlAttribute(name = "CachedPlanSize")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger cachedPlanSize;
    @XmlAttribute(name = "CompileTime")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger compileTime;
    @XmlAttribute(name = "CompileCPU")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger compileCPU;
    @XmlAttribute(name = "CompileMemory")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger compileMemory;
    @XmlAttribute(name = "UsePlan")
    protected Boolean usePlan;

    /**
     * Gets the value of the internalInfo property.
     * 
     * @return
     *     possible object is
     *     {@link InternalInfoType_sql2008 }
     *     
     */
    public InternalInfoType_sql2008 getInternalInfo() {
        return internalInfo;
    }

    /**
     * Sets the value of the internalInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternalInfoType_sql2008 }
     *     
     */
    public void setInternalInfo(InternalInfoType_sql2008 value) {
        this.internalInfo = value;
    }

    /**
     * Gets the value of the missingIndexes property.
     * 
     * @return
     *     possible object is
     *     {@link MissingIndexesType_sql2008 }
     *     
     */
    public MissingIndexesType_sql2008 getMissingIndexes() {
        return missingIndexes;
    }

    /**
     * Sets the value of the missingIndexes property.
     * 
     * @param value
     *     allowed object is
     *     {@link MissingIndexesType_sql2008 }
     *     
     */
    public void setMissingIndexes(MissingIndexesType_sql2008 value) {
        this.missingIndexes = value;
    }

    /**
     * Gets the value of the guessedSelectivity property.
     * 
     * @return
     *     possible object is
     *     {@link GuessedSelectivityType_sql2008 }
     *     
     */
    public GuessedSelectivityType_sql2008 getGuessedSelectivity() {
        return guessedSelectivity;
    }

    /**
     * Sets the value of the guessedSelectivity property.
     * 
     * @param value
     *     allowed object is
     *     {@link GuessedSelectivityType_sql2008 }
     *     
     */
    public void setGuessedSelectivity(GuessedSelectivityType_sql2008 value) {
        this.guessedSelectivity = value;
    }

    /**
     * Gets the value of the unmatchedIndexes property.
     * 
     * @return
     *     possible object is
     *     {@link UnmatchedIndexesType_sql2008 }
     *     
     */
    public UnmatchedIndexesType_sql2008 getUnmatchedIndexes() {
        return unmatchedIndexes;
    }

    /**
     * Sets the value of the unmatchedIndexes property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnmatchedIndexesType_sql2008 }
     *     
     */
    public void setUnmatchedIndexes(UnmatchedIndexesType_sql2008 value) {
        this.unmatchedIndexes = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public RelOpType_sql2008 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public void setRelOp(RelOpType_sql2008 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the parameterList property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getParameterList() {
        return parameterList;
    }

    /**
     * Sets the value of the parameterList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setParameterList(ColumnReferenceListType_sql2008 value) {
        this.parameterList = value;
    }

    /**
     * Gets the value of the degreeOfParallelism property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDegreeOfParallelism() {
        return degreeOfParallelism;
    }

    /**
     * Sets the value of the degreeOfParallelism property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDegreeOfParallelism(Integer value) {
        this.degreeOfParallelism = value;
    }

    /**
     * Gets the value of the memoryGrant property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMemoryGrant() {
        return memoryGrant;
    }

    /**
     * Sets the value of the memoryGrant property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMemoryGrant(BigInteger value) {
        this.memoryGrant = value;
    }

    /**
     * Gets the value of the cachedPlanSize property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCachedPlanSize() {
        return cachedPlanSize;
    }

    /**
     * Sets the value of the cachedPlanSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCachedPlanSize(BigInteger value) {
        this.cachedPlanSize = value;
    }

    /**
     * Gets the value of the compileTime property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCompileTime() {
        return compileTime;
    }

    /**
     * Sets the value of the compileTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCompileTime(BigInteger value) {
        this.compileTime = value;
    }

    /**
     * Gets the value of the compileCPU property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCompileCPU() {
        return compileCPU;
    }

    /**
     * Sets the value of the compileCPU property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCompileCPU(BigInteger value) {
        this.compileCPU = value;
    }

    /**
     * Gets the value of the compileMemory property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCompileMemory() {
        return compileMemory;
    }

    /**
     * Sets the value of the compileMemory property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCompileMemory(BigInteger value) {
        this.compileMemory = value;
    }

    /**
     * Gets the value of the usePlan property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getUsePlan() {
        return usePlan;
    }

    /**
     * Sets the value of the usePlan property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUsePlan(Boolean value) {
        this.usePlan = value;
    }

}
