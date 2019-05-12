
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * the type that contains the basic statement information
 * 
 * <p>Java class for BaseStmtInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BaseStmtInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="StatementSetOptions" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SetOptionsType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="StatementCompId" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="StatementEstRows" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="StatementId" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="StatementOptmLevel" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="StatementOptmEarlyAbortReason">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="TimeOut"/>
 *             &lt;enumeration value="MemoryLimitExceeded"/>
 *             &lt;enumeration value="GoodEnoughPlanFound"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="StatementSubTreeCost" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="StatementText" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="StatementType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="TemplatePlanGuideDB" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="TemplatePlanGuideName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PlanGuideDB" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PlanGuideName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseStmtInfoType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "statementSetOptions"
})
@XmlSeeAlso({
    StmtSimpleType_sql2005 .class,
    StmtCondType_sql2005 .class,
    StmtCursorType_sql2005 .class,
    StmtReceiveType_sql2005 .class,
    StmtUseDbType_sql2005 .class
})
public class BaseStmtInfoType_sql2005 {

    @XmlElement(name = "StatementSetOptions", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SetOptionsType_sql2005 statementSetOptions;
    @XmlAttribute(name = "StatementCompId")
    protected Integer statementCompId;
    @XmlAttribute(name = "StatementEstRows")
    protected Double statementEstRows;
    @XmlAttribute(name = "StatementId")
    protected Integer statementId;
    @XmlAttribute(name = "StatementOptmLevel")
    protected String statementOptmLevel;
    @XmlAttribute(name = "StatementOptmEarlyAbortReason")
    protected String statementOptmEarlyAbortReason;
    @XmlAttribute(name = "StatementSubTreeCost")
    protected Double statementSubTreeCost;
    @XmlAttribute(name = "StatementText")
    protected String statementText;
    @XmlAttribute(name = "StatementType")
    protected String statementType;
    @XmlAttribute(name = "TemplatePlanGuideDB")
    protected String templatePlanGuideDB;
    @XmlAttribute(name = "TemplatePlanGuideName")
    protected String templatePlanGuideName;
    @XmlAttribute(name = "PlanGuideDB")
    protected String planGuideDB;
    @XmlAttribute(name = "PlanGuideName")
    protected String planGuideName;

    /**
     * Gets the value of the statementSetOptions property.
     * 
     * @return
     *     possible object is
     *     {@link SetOptionsType_sql2005 }
     *     
     */
    public SetOptionsType_sql2005 getStatementSetOptions() {
        return statementSetOptions;
    }

    /**
     * Sets the value of the statementSetOptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link SetOptionsType_sql2005 }
     *     
     */
    public void setStatementSetOptions(SetOptionsType_sql2005 value) {
        this.statementSetOptions = value;
    }

    /**
     * Gets the value of the statementCompId property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getStatementCompId() {
        return statementCompId;
    }

    /**
     * Sets the value of the statementCompId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStatementCompId(Integer value) {
        this.statementCompId = value;
    }

    /**
     * Gets the value of the statementEstRows property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getStatementEstRows() {
        return statementEstRows;
    }

    /**
     * Sets the value of the statementEstRows property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setStatementEstRows(Double value) {
        this.statementEstRows = value;
    }

    /**
     * Gets the value of the statementId property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getStatementId() {
        return statementId;
    }

    /**
     * Sets the value of the statementId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setStatementId(Integer value) {
        this.statementId = value;
    }

    /**
     * Gets the value of the statementOptmLevel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatementOptmLevel() {
        return statementOptmLevel;
    }

    /**
     * Sets the value of the statementOptmLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatementOptmLevel(String value) {
        this.statementOptmLevel = value;
    }

    /**
     * Gets the value of the statementOptmEarlyAbortReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatementOptmEarlyAbortReason() {
        return statementOptmEarlyAbortReason;
    }

    /**
     * Sets the value of the statementOptmEarlyAbortReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatementOptmEarlyAbortReason(String value) {
        this.statementOptmEarlyAbortReason = value;
    }

    /**
     * Gets the value of the statementSubTreeCost property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getStatementSubTreeCost() {
        return statementSubTreeCost;
    }

    /**
     * Sets the value of the statementSubTreeCost property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setStatementSubTreeCost(Double value) {
        this.statementSubTreeCost = value;
    }

    /**
     * Gets the value of the statementText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatementText() {
        return statementText;
    }

    /**
     * Sets the value of the statementText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatementText(String value) {
        this.statementText = value;
    }

    /**
     * Gets the value of the statementType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatementType() {
        return statementType;
    }

    /**
     * Sets the value of the statementType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatementType(String value) {
        this.statementType = value;
    }

    /**
     * Gets the value of the templatePlanGuideDB property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTemplatePlanGuideDB() {
        return templatePlanGuideDB;
    }

    /**
     * Sets the value of the templatePlanGuideDB property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTemplatePlanGuideDB(String value) {
        this.templatePlanGuideDB = value;
    }

    /**
     * Gets the value of the templatePlanGuideName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTemplatePlanGuideName() {
        return templatePlanGuideName;
    }

    /**
     * Sets the value of the templatePlanGuideName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTemplatePlanGuideName(String value) {
        this.templatePlanGuideName = value;
    }

    /**
     * Gets the value of the planGuideDB property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlanGuideDB() {
        return planGuideDB;
    }

    /**
     * Sets the value of the planGuideDB property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlanGuideDB(String value) {
        this.planGuideDB = value;
    }

    /**
     * Gets the value of the planGuideName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlanGuideName() {
        return planGuideName;
    }

    /**
     * Sets the value of the planGuideName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlanGuideName(String value) {
        this.planGuideName = value;
    }

}
