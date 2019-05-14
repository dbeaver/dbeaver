
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				The Adaptive Join element replaces a adaptive concat with Hash Join and Nested loops as inputs. This element
 * 				will have 3 inputs the two children of the HJ and the inner child of the NLJ. We append the required HJ and NLJ properties to the new 
 * 				AdaptiveJoin showplan element.
 * 			
 * 
 * <p>Java class for AdaptiveJoinType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AdaptiveJoinType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="HashKeysBuild" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="HashKeysProbe" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="BuildResidual" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="ProbeResidual" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="StarJoinInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StarJoinInfoType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="PassThru" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="OuterReferences" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="PartitionId" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" maxOccurs="3" minOccurs="3"/>
 *       &lt;/sequence>
 *       &lt;attribute name="BitmapCreator" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="Optimized" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdaptiveJoinType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "hashKeysBuild",
    "hashKeysProbe",
    "buildResidual",
    "probeResidual",
    "starJoinInfo",
    "predicate",
    "passThru",
    "outerReferences",
    "partitionId",
    "relOp"
})
public class AdaptiveJoinType_sql2017
    extends RelOpBaseType_sql2017 
{

    @XmlElement(name = "HashKeysBuild", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2017 hashKeysBuild;
    @XmlElement(name = "HashKeysProbe", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2017 hashKeysProbe;
    @XmlElement(name = "BuildResidual", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 buildResidual;
    @XmlElement(name = "ProbeResidual", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 probeResidual;
    @XmlElement(name = "StarJoinInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StarJoinInfoType_sql2017 starJoinInfo;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 predicate;
    @XmlElement(name = "PassThru", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 passThru;
    @XmlElement(name = "OuterReferences", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2017 outerReferences;
    @XmlElement(name = "PartitionId", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2017 partitionId;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RelOpType_sql2017> relOp;
    @XmlAttribute(name = "BitmapCreator")
    protected Boolean bitmapCreator;
    @XmlAttribute(name = "Optimized", required = true)
    protected boolean optimized;

    /**
     * Gets the value of the hashKeysBuild property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public ColumnReferenceListType_sql2017 getHashKeysBuild() {
        return hashKeysBuild;
    }

    /**
     * Sets the value of the hashKeysBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public void setHashKeysBuild(ColumnReferenceListType_sql2017 value) {
        this.hashKeysBuild = value;
    }

    /**
     * Gets the value of the hashKeysProbe property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public ColumnReferenceListType_sql2017 getHashKeysProbe() {
        return hashKeysProbe;
    }

    /**
     * Sets the value of the hashKeysProbe property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public void setHashKeysProbe(ColumnReferenceListType_sql2017 value) {
        this.hashKeysProbe = value;
    }

    /**
     * Gets the value of the buildResidual property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getBuildResidual() {
        return buildResidual;
    }

    /**
     * Sets the value of the buildResidual property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setBuildResidual(ScalarExpressionType_sql2017 value) {
        this.buildResidual = value;
    }

    /**
     * Gets the value of the probeResidual property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getProbeResidual() {
        return probeResidual;
    }

    /**
     * Sets the value of the probeResidual property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setProbeResidual(ScalarExpressionType_sql2017 value) {
        this.probeResidual = value;
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
     * Gets the value of the bitmapCreator property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getBitmapCreator() {
        return bitmapCreator;
    }

    /**
     * Sets the value of the bitmapCreator property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBitmapCreator(Boolean value) {
        this.bitmapCreator = value;
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

}
