<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions"
                exclude-result-prefixes="f">

    <!-- Parameters -->
    <xsl:param name="printTargetRole"/>
    <xsl:param name="addSuperType"/>
    <xsl:param name="addAssociations"/>
    <xsl:param name="addProductCmptType"/>
    <xsl:param name="showPolicyComponents"/>
    <xsl:param name="showProductComponents"/>
    <xsl:param name="showTables"/>
    <xsl:param name="showEnumTypes"/>
    <xsl:param name="showEnumAssociations"/>
    <xsl:param name="showTableUsage"/>
    <xsl:param name="packages"/>
    <xsl:param name="connector" select="'--'"/>
    <xsl:param name="dottedConnector" select="'..'"/>
    <xsl:param name="packageFilter"/>
    <xsl:param name="showEnumContent"/>
    <xsl:param name="showMavenModule"/>
    <xsl:param name="showDescriptions"/>
    <xsl:param name="descriptionLocale" select="'de'"/>
    <xsl:param name="showCardinalities"/>
    <xsl:param name="sortAttributesAlphabetically"/>

    <!-- Constants -->
    <xsl:variable name="policySpot">(V,lightSteelBlue)</xsl:variable>
    <xsl:variable name="productSpot">(P,deepSkyBlue)</xsl:variable>
    <xsl:variable name="tableStructureSpot">(T,LightSalmon)</xsl:variable>
    <xsl:variable name="policyType">PolicyCmptType</xsl:variable>
    <xsl:variable name="productType">ProductCmptType</xsl:variable>
    <xsl:variable name="ff">&gt;&gt;</xsl:variable>
    <xsl:variable name="bb">&lt;&lt;</xsl:variable>
    <xsl:variable name="singleQuote">'</xsl:variable>

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <!-- Module includes -->
    <xsl:include href="helpers.xsl"/>
    <xsl:include href="relationships.xsl"/>
    <xsl:include href="component-types.xsl"/>
    <xsl:include href="enum-types.xsl"/>
    <xsl:include href="table-structures.xsl"/>

    <!-- Root template: wraps output in PlantUML diagram markers -->
    <xsl:template match="/">
        <xsl:text>@startuml&#xa;</xsl:text>
        <xsl:text>hide empty members&#xa;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>@enduml</xsl:text>
    </xsl:template>

    <!-- Ignore content-only element types -->
    <xsl:template match="ProductCmpt|ProductVariant|TableContents"/>

</xsl:stylesheet>