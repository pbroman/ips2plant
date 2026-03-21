<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Checks if a class name matches the package filter (comma-separated list) -->
    <xsl:template name="matches-package-filter">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="filters" select="$packageFilter"/>
        <xsl:choose>
            <xsl:when test="$filters = ''">true</xsl:when>
            <xsl:when test="contains($filters, ',')">
                <xsl:variable name="first" select="normalize-space(substring-before($filters, ','))"/>
                <xsl:variable name="rest" select="substring-after($filters, ',')"/>
                <xsl:variable name="firstMatch">
                    <xsl:choose>
                        <xsl:when test="starts-with($classNameWithPackage, $first)">true</xsl:when>
                        <xsl:otherwise>false</xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$firstMatch = 'true'">true</xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="matches-package-filter">
                            <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                            <xsl:with-param name="filters" select="$rest"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="starts-with($classNameWithPackage, normalize-space($filters))">true</xsl:when>
            <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Selects the class name with or without package prefix depending on $packages param -->
    <xsl:template name="packaging-selector">
        <xsl:param name="clazz"/>
        <xsl:choose>
            <xsl:when test="$packages">
                <xsl:value-of select="$clazz"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="$clazz" />
                    <xsl:with-param name="delimiter" select="'.'" />
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Returns the substring after the last occurrence of delimiter -->
    <xsl:template name="substring-after-last">
        <xsl:param name="string" />
        <xsl:param name="delimiter" />
        <xsl:choose>
            <xsl:when test="contains($string, $delimiter)">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="substring-after($string, $delimiter)" />
                    <xsl:with-param name="delimiter" select="$delimiter" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$string" /></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        Determines whether a relationship (composition, association, aggregation) should be shown.
        A relationship is visible when:
        1. The target is present in the model OR addAssociations is enabled, AND
        2. Both source and target pass the package filter (or addAssociations allows cross-filter visibility)
    -->
    <xsl:template name="is-relationship-visible">
        <xsl:param name="isTargetPresent"/>
        <xsl:param name="sourceWithPackage"/>
        <xsl:param name="targetWithPackage"/>
        <xsl:variable name="sourceMatchesFilter">
            <xsl:call-template name="matches-package-filter">
                <xsl:with-param name="classNameWithPackage" select="$sourceWithPackage"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="targetMatchesFilter">
            <xsl:call-template name="matches-package-filter">
                <xsl:with-param name="classNameWithPackage" select="$targetWithPackage"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="($isTargetPresent != '' or $addAssociations = 'true')
                            and (($sourceMatchesFilter = 'true' or $targetMatchesFilter = 'true') and $addAssociations = 'true'
                                 or ($sourceMatchesFilter = 'true' and $targetMatchesFilter = 'true'))">1</xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

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