
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for NestedLoopsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NestedLoopsType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="PassThru" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="OuterReferences" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="PartitionId" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="ProbeColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="StarJoinInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StarJoinInfoType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" maxOccurs="2" minOccurs="2"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Optimized" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="WithOrderedPrefetch" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="WithUnorderedPrefetch" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NestedLoopsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "predicate",
    "passThru",
    "outerReferences",
    "partitionId",
    "probeColumn",
    "starJoinInfo",
    "relOp"
})
public class NestedLoopsType_sql2017
    extends RelOpBaseType_sql2017
{

    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 predicate;
    @XmlElement(name = "PassThru", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 passThru;
    @XmlElement(name = "OuterReferences", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2017 outerReferences;
    @XmlElement(name = "PartitionId", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2017 partitionId;
    @XmlElement(name = "ProbeColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2017 probeColumn;
    @XmlElement(name = "StarJoinInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StarJoinInfoType_sql2017 starJoinInfo;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RelOpType_sql2017> relOp;
    @XmlAttribute(name = "Optimized", required = true)
    protected boolean optimized;
    @XmlAttribute(name = "WithOrderedPrefetch")
    protected Boolean withOrderedPrefetch;
    @XmlAttribute(name = "WithUnorderedPrefetch")
    protected Boolean withUnorderedPrefetch;

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2017 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the passThru property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getPassThru() {
        return passThru;
    }

    /**
     * Sets the value of the passThru property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setPassThru(ScalarExpressionType_sql2017 value) {
        this.passThru = value;
    }

    /**
     * Gets the value of the outerReferences property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public ColumnReferenceListType_sql2017 getOuterReferences() {
        return outerReferences;
    }

    /**
     * Sets the value of the outerReferences property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public void setOuterReferences(ColumnReferenceListType_sql2017 value) {
        this.outerReferences = value;
    }

    /**
     * Gets the value of the partitionId property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2017 }
     *     
     */
    public SingleColumnReferenceType_sql2017 getPartitionId() {
        return partitionId;
    }

    /**
     * Sets the value of the partitionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2017 }
     *     
     */
    public void setPartitionId(SingleColumnReferenceType_sql2017 value) {
        this.partitionId = value;
    }

    /**
     * Gets the value of the probeColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2017 }
     *     
     */
    public SingleColumnReferenceType_sql2017 getProbeColumn() {
        return probeColumn;
    }

    /**
     * Sets the value of the probeColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2017 }
     *     
     */
    public void setProbeColumn(SingleColumnReferenceType_sql2017 value) {
        this.probeColumn = value;
    }

    /**
     * Gets the value of the starJoinInfo property.
     * 
     * @return
     *     possible object is
     *     {@link StarJoinInfoType_sql2017 }
     *     
     */
    public StarJoinInfoType_sql2017 getStarJoinInfo() {
        return starJoinInfo;
    }

    /**
     * Sets the value of the starJoinInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link StarJoinInfoType_sql2017 }
     *     
     */
    public void setStarJoinInfo(StarJoinInfoType_sql2017 value) {
        this.starJoinInfo = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relOp property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelOp().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RelOpType_sql2017 }
     * 
     * 
     */
    public List<RelOpType_sql2017> getRelOp() {
        if (relOp == null) {
            relOp = new ArrayList<RelOpType_sql2017>();
        }
        return this.relOp;
    }

    /**
     * Gets the value of the optimized property.
     * 
     */
    public boolean isOptimized() {
        return optimized;
    }

    /**
     * Sets the value of the optimized property.
     * 
     */
    public void setOptimized(boolean value) {
        this.optimized = value;
    }

    /**
     * Gets the value of the withOrderedPrefetch property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getWithOrderedPrefetch() {
        return withOrderedPrefetch;
    }

    /**
     * Sets the value of the withOrderedPrefetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithOrderedPrefetch(Boolean value) {
        this.withOrderedPrefetch = value;
    }

    /**
     * Gets the value of the withUnorderedPrefetch property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getWithUnorderedPrefetch() {
        return withUnorderedPrefetch;
    }

    /**
     * Sets the value of the withUnorderedPrefetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithUnorderedPrefetch(Boolean value) {
        this.withUnorderedPrefetch = value;
    }

}
