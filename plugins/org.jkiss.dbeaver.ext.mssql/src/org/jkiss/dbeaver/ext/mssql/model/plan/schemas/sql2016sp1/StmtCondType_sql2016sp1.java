
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Complex statement type that is constructed by a condition, a then clause and an optional else clause. 
 * 
 * <p>Java class for StmtCondType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StmtCondType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}BaseStmtInfoType">
 *       &lt;sequence>
 *         &lt;element name="Condition">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType" minOccurs="0"/>
 *                   &lt;element name="UDF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Then">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Else" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StmtCondType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "condition",
    "then",
    "_else"
})
public class StmtCondType_sql2016sp1
    extends BaseStmtInfoType_sql2016sp1
{

    @XmlElement(name = "Condition", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected StmtCondType_sql2016sp1 .Condition_sql2016sp1 condition;
    @XmlElement(name = "Then", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected StmtCondType_sql2016sp1 .Then_sql2016sp1 then;
    @XmlElement(name = "Else", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected StmtCondType_sql2016sp1 .Else_sql2016sp1 _else;

    /**
     * Gets the value of the condition property.
     * 
     * @return
     *     possible object is
     *     {@link StmtCondType_sql2016sp1 .Condition_sql2016sp1 }
     *     
     */
    public StmtCondType_sql2016sp1 .Condition_sql2016sp1 getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     * 
     * @param value
     *     allowed object is
     *     {@link StmtCondType_sql2016sp1 .Condition_sql2016sp1 }
     *     
     */
    public void setCondition(StmtCondType_sql2016sp1 .Condition_sql2016sp1 value) {
        this.condition = value;
    }

    /**
     * Gets the value of the then property.
     * 
     * @return
     *     possible object is
     *     {@link StmtCondType_sql2016sp1 .Then_sql2016sp1 }
     *     
     */
    public StmtCondType_sql2016sp1 .Then_sql2016sp1 getThen() {
        return then;
    }

    /**
     * Sets the value of the then property.
     * 
     * @param value
     *     allowed object is
     *     {@link StmtCondType_sql2016sp1 .Then_sql2016sp1 }
     *     
     */
    public void setThen(StmtCondType_sql2016sp1 .Then_sql2016sp1 value) {
        this.then = value;
    }

    /**
     * Gets the value of the else property.
     * 
     * @return
     *     possible object is
     *     {@link StmtCondType_sql2016sp1 .Else_sql2016sp1 }
     *     
     */
    public StmtCondType_sql2016sp1 .Else_sql2016sp1 getElse() {
        return _else;
    }

    /**
     * Sets the value of the else property.
     * 
     * @param value
     *     allowed object is
     *     {@link StmtCondType_sql2016sp1 .Else_sql2016sp1 }
     *     
     */
    public void setElse(StmtCondType_sql2016sp1 .Else_sql2016sp1 value) {
        this._else = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType" minOccurs="0"/>
     *         &lt;element name="UDF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "queryPlan",
        "udf"
    })
    public static class Condition_sql2016sp1 {

        @XmlElement(name = "QueryPlan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected QueryPlanType_sql2016sp1 queryPlan;
        @XmlElement(name = "UDF", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected List<FunctionType_sql2016sp1> udf;

        /**
         * Gets the value of the queryPlan property.
         * 
         * @return
         *     possible object is
         *     {@link QueryPlanType_sql2016sp1 }
         *     
         */
        public QueryPlanType_sql2016sp1 getQueryPlan() {
            return queryPlan;
        }

        /**
         * Sets the value of the queryPlan property.
         * 
         * @param value
         *     allowed object is
         *     {@link QueryPlanType_sql2016sp1 }
         *     
         */
        public void setQueryPlan(QueryPlanType_sql2016sp1 value) {
            this.queryPlan = value;
        }

        /**
         * Gets the value of the udf property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the udf property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getUDF().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link FunctionType_sql2016sp1 }
         * 
         * 
         */
        public List<FunctionType_sql2016sp1> getUDF() {
            if (udf == null) {
                udf = new ArrayList<FunctionType_sql2016sp1>();
            }
            return this.udf;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "statements"
    })
    public static class Else_sql2016sp1 {

        @XmlElement(name = "Statements", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected StmtBlockType_sql2016sp1 statements;

        /**
         * Gets the value of the statements property.
         * 
         * @return
         *     possible object is
         *     {@link StmtBlockType_sql2016sp1 }
         *     
         */
        public StmtBlockType_sql2016sp1 getStatements() {
            return statements;
        }

        /**
         * Sets the value of the statements property.
         * 
         * @param value
         *     allowed object is
         *     {@link StmtBlockType_sql2016sp1 }
         *     
         */
        public void setStatements(StmtBlockType_sql2016sp1 value) {
            this.statements = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "statements"
    })
    public static class Then_sql2016sp1 {

        @XmlElement(name = "Statements", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected StmtBlockType_sql2016sp1 statements;

        /**
         * Gets the value of the statements property.
         * 
         * @return
         *     possible object is
         *     {@link StmtBlockType_sql2016sp1 }
         *     
         */
        public StmtBlockType_sql2016sp1 getStatements() {
            return statements;
        }

        /**
         * Sets the value of the statements property.
         * 
         * @param value
         *     allowed object is
         *     {@link StmtBlockType_sql2016sp1 }
         *     
         */
        public void setStatements(StmtBlockType_sql2016sp1 value) {
            this.statements = value;
        }

    }

}
