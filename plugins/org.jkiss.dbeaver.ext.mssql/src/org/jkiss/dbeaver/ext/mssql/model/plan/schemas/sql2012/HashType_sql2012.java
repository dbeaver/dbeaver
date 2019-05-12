
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HashType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HashType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="HashKeysBuild" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="HashKeysProbe" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="BuildResidual" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="ProbeResidual" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="StarJoinInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StarJoinInfoType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" maxOccurs="2"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HashType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "hashKeysBuild",
    "hashKeysProbe",
    "buildResidual",
    "probeResidual",
    "starJoinInfo",
    "relOp"
})
public class HashType_sql2012
    extends RelOpBaseType_sql2012
{

    @XmlElement(name = "HashKeysBuild", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2012 hashKeysBuild;
    @XmlElement(name = "HashKeysProbe", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2012 hashKeysProbe;
    @XmlElement(name = "BuildResidual", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2012 buildResidual;
    @XmlElement(name = "ProbeResidual", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2012 probeResidual;
    @XmlElement(name = "StarJoinInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StarJoinInfoType_sql2012 starJoinInfo;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RelOpType_sql2012> relOp;

    /**
     * Gets the value of the hashKeysBuild property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2012 }
     *     
     */
    public ColumnReferenceListType_sql2012 getHashKeysBuild() {
        return hashKeysBuild;
    }

    /**
     * Sets the value of the hashKeysBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2012 }
     *     
     */
    public void setHashKeysBuild(ColumnReferenceListType_sql2012 value) {
        this.hashKeysBuild = value;
    }

    /**
     * Gets the value of the hashKeysProbe property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2012 }
     *     
     */
    public ColumnReferenceListType_sql2012 getHashKeysProbe() {
        return hashKeysProbe;
    }

    /**
     * Sets the value of the hashKeysProbe property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2012 }
     *     
     */
    public void setHashKeysProbe(ColumnReferenceListType_sql2012 value) {
        this.hashKeysProbe = value;
    }

    /**
     * Gets the value of the buildResidual property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public ScalarExpressionType_sql2012 getBuildResidual() {
        return buildResidual;
    }

    /**
     * Sets the value of the buildResidual property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public void setBuildResidual(ScalarExpressionType_sql2012 value) {
        this.buildResidual = value;
    }

    /**
     * Gets the value of the probeResidual property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public ScalarExpressionType_sql2012 getProbeResidual() {
        return probeResidual;
    }

    /**
     * Sets the value of the probeResidual property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2012 }
     *     
     */
    public void setProbeResidual(ScalarExpressionType_sql2012 value) {
        this.probeResidual = value;
    }

    /**
     * Gets the value of the starJoinInfo property.
     * 
     * @return
     *     possible object is
     *     {@link StarJoinInfoType_sql2012 }
     *     
     */
    public StarJoinInfoType_sql2012 getStarJoinInfo() {
        return starJoinInfo;
    }

    /**
     * Sets the value of the starJoinInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link StarJoinInfoType_sql2012 }
     *     
     */
    public void setStarJoinInfo(StarJoinInfoType_sql2012 value) {
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
     * {@link RelOpType_sql2012 }
     * 
     * 
     */
    public List<RelOpType_sql2012> getRelOp() {
        if (relOp == null) {
            relOp = new ArrayList<RelOpType_sql2012>();
        }
        return this.relOp;
    }

}
