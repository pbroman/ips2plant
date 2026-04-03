<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <!-- Renders an enum type association arrow (dotted arrow to enum) -->
    <xsl:template name="enum-association">
        <xsl:param name="enumType" />
        <xsl:param name="className" />
        <xsl:variable name="isEnumPresent">
            <xsl:value-of select="../../EnumType[@className=$enumType]"/>
        </xsl:variable>
        <xsl:if test="$isEnumPresent != '' and $showEnumAssociations">
            <xsl:value-of select="concat($className, ' ', $dottedConnector, '> ', f:class-name($enumType), '&#xa;')"/>
        </xsl:if>
    </xsl:template>

    <!-- Renders a table usage arrow (dotted arrow to table) -->
    <xsl:template name="table-usage">
        <xsl:param name="tableStructure" />
        <xsl:param name="className" />
        <xsl:if test="f:matches-package-filter($className) and $showTableUsage">
            <xsl:value-of select="concat($className, ' ', $dottedConnector, '{ ', f:class-name($tableStructure), '&#xa;')"/>
        </xsl:if>
    </xsl:template>

    <!-- Renders inheritance arrow: SuperType extends ClassName -->
    <xsl:template name="inheritance">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="className"/>
        <xsl:param name="supertypeAttr"/>
        <xsl:param name="isSupertypePresent"/>

        <xsl:if test="$supertypeAttr and f:matches-package-filter($classNameWithPackage)">
            <xsl:if test="($isSupertypePresent != '' and f:matches-package-filter($supertypeAttr)) or $addSuperType = 'true'">
                <xsl:value-of select="concat(f:class-name($supertypeAttr), ' &lt;|', $connector, ' ', $className, '&#xa;')"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- Renders composition arrows -->
    <xsl:template name="compositions">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="className"/>

        <xsl:for-each select="Association[@associationType='comp']">
            <xsl:variable name="targetWithPackage" select="@target"/>
            <xsl:variable name="isCompTargetPresent">
                <xsl:value-of select="../../PolicyCmptType[@className=$targetWithPackage]"/>
            </xsl:variable>

            <xsl:if test="f:is-relationship-visible($isCompTargetPresent, $classNameWithPackage, $targetWithPackage)">
                <xsl:variable name="target" select="f:class-name(@target)"/>

                <xsl:variable name="targetMin">
                    <xsl:value-of
                            select="../../PolicyCmptType[@className=$targetWithPackage]/Association[@associationType='reverseComp' and @target=$classNameWithPackage]/@minCardinality"/>
                </xsl:variable>
                <xsl:variable name="targetMax">
                    <xsl:value-of
                            select="../../PolicyCmptType[@className=$targetWithPackage]/Association[@associationType='reverseComp' and @target=$classNameWithPackage]/@maxCardinality"/>
                </xsl:variable>

                <xsl:value-of select="$className"/>
                <xsl:if test="$targetMin != '' and $targetMax != ''">
                    <xsl:value-of select="concat(' &quot;', $targetMin, '..', $targetMax, '&quot; ')"/>
                </xsl:if>
                <xsl:value-of
                    select="concat(' *', $connector, ' &quot;', @minCardinality, '..', @maxCardinality, '&quot; ', $target)"/>
                <xsl:call-template name="target-role-label"/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!-- Renders association arrows: Target .. ClassName -->
    <xsl:template name="associations">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="className"/>
        <xsl:param name="componentType"/>

        <xsl:for-each select="Association[@associationType='ass']">
            <xsl:variable name="targetWithPackage" select="@target"/>
            <xsl:variable name="isTargetPresent">
                <xsl:call-template name="is-target-present">
                    <xsl:with-param name="componentType" select="$componentType"/>
                    <xsl:with-param name="targetClassName" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="f:is-relationship-visible($isTargetPresent, $classNameWithPackage, $targetWithPackage)">
                <xsl:value-of select="concat(f:class-name(@target), ' &lt;', $connector, ' ', $className)"/>
                <xsl:call-template name="target-role-label"/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!-- Renders aggregation arrows -->
    <xsl:template name="aggregations">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="className"/>
        <xsl:param name="componentType"/>

        <xsl:for-each select="Association[@associationType='aggr']">
            <xsl:variable name="targetWithPackage" select="@target"/>
            <xsl:variable name="isTargetPresent">
                <xsl:call-template name="is-target-present">
                    <xsl:with-param name="componentType" select="$componentType"/>
                    <xsl:with-param name="targetClassName" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="f:is-relationship-visible($isTargetPresent, $classNameWithPackage, $targetWithPackage)">
                <xsl:value-of
                    select="concat($className, ' o', $connector, ' &quot;', @minCardinality, '..', @maxCardinality, '&quot; ', f:class-name(@target))"/>
                <xsl:call-template name="target-role-label"/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
