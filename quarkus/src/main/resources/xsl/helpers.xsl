<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <!-- Checks if a class name matches the package filter (comma-separated list) -->
    <xsl:function name="f:matches-package-filter" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="classNameWithPackage" as="xs:string"/>
        <xsl:param name="filters" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$filters = ''"><xsl:sequence select="true()"/></xsl:when>
            <xsl:when test="contains($filters, ',')">
                <xsl:sequence select="
                    if (starts-with($classNameWithPackage, normalize-space(substring-before($filters, ','))))
                    then true()
                    else f:matches-package-filter($classNameWithPackage, substring-after($filters, ','))
                "/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:sequence select="starts-with($classNameWithPackage, normalize-space($filters))"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Convenience overload using the global $packageFilter param -->
    <xsl:function name="f:matches-package-filter" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="classNameWithPackage" as="xs:string"/>
        <xsl:sequence select="f:matches-package-filter($classNameWithPackage, $packageFilter)"/>
    </xsl:function>

    <!-- Selects the class name with or without package prefix depending on $packages param -->
    <xsl:function name="f:class-name" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="clazz" as="xs:string"/>
        <xsl:sequence select="
            if ($packages) then $clazz
            else f:substring-after-last($clazz, '.')
        "/>
    </xsl:function>

    <!-- Returns the substring after the last occurrence of delimiter -->
    <xsl:function name="f:substring-after-last" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="string" as="xs:string"/>
        <xsl:param name="delimiter" as="xs:string"/>
        <xsl:sequence select="
            if (contains($string, $delimiter))
            then f:substring-after-last(substring-after($string, $delimiter), $delimiter)
            else $string
        "/>
    </xsl:function>

    <!--
        Determines whether a relationship (composition, association, aggregation) should be shown.
        A relationship is visible when:
        1. The target is present in the model OR addAssociations is enabled, AND
        2. Both source and target pass the package filter (or addAssociations allows cross-filter visibility)
    -->
    <xsl:function name="f:is-relationship-visible" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="isTargetPresent" as="xs:string"/>
        <xsl:param name="sourceWithPackage" as="xs:string"/>
        <xsl:param name="targetWithPackage" as="xs:string"/>
        <xsl:variable name="sourceMatch" select="f:matches-package-filter($sourceWithPackage)"/>
        <xsl:variable name="targetMatch" select="f:matches-package-filter($targetWithPackage)"/>
        <xsl:sequence select="
            ($isTargetPresent != '' or $addAssociations = 'true')
            and (($sourceMatch or $targetMatch) and $addAssociations = 'true'
                 or ($sourceMatch and $targetMatch))
        "/>
    </xsl:function>

    <!-- Checks whether a target class exists in the model for the given component type -->
    <xsl:template name="is-target-present">
        <xsl:param name="componentType"/>
        <xsl:param name="targetClassName"/>
        <xsl:choose>
            <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../../PolicyCmptType[@className=$targetClassName]"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="../../ProductCmptType2[@className=$targetClassName]"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Renders the target role label if enabled -->
    <xsl:template name="target-role-label">
        <xsl:if test="@targetRoleSingular and $printTargetRole = 'true'">
            <xsl:value-of select="concat(' : ', @targetRoleSingular)"/>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
