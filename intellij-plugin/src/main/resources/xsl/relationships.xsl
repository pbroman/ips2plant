<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Renders an enum type association arrow (dotted arrow to enum) -->
    <xsl:template name="enum-association">
        <xsl:param name="enumType" />
        <xsl:param name="className" />
        <xsl:variable name="isEnumPresent">
            <xsl:value-of select="../../EnumType[@className=$enumType]"/>
        </xsl:variable>
        <xsl:variable name="enumTypePackaging">
            <xsl:call-template name="packaging-selector">
                <xsl:with-param name="clazz" select="$enumType" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="$isEnumPresent != '' and $showEnumAssociations">
            <xsl:value-of select="concat($className, ' ', $dottedConnector, '> ', $enumTypePackaging, '&#xa;')"/>
        </xsl:if>
    </xsl:template>

    <!-- Renders a table usage arrow (dotted arrow to table) -->
    <xsl:template name="table-usage">
        <xsl:param name="tableStructure" />
        <xsl:param name="className" />
        <xsl:variable name="isTablePresent">
            <xsl:value-of select="../../../TableStructure[@className=$tableStructure]"/>
        </xsl:variable>
        <xsl:variable name="tableStructurePackaging">
            <xsl:call-template name="packaging-selector">
                <xsl:with-param name="clazz" select="$tableStructure" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="tableClassMatchesFilter">
            <xsl:call-template name="matches-package-filter">
                <xsl:with-param name="classNameWithPackage" select="$className"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="$tableClassMatchesFilter = 'true' and $showTableUsage">
            <xsl:value-of select="concat($className, ' ', $dottedConnector, '{ ', $tableStructurePackaging, '&#xa;')"/>
        </xsl:if>
    </xsl:template>

    <!-- Renders inheritance arrow: SuperType extends ClassName -->
    <xsl:template name="inheritance">
        <xsl:param name="classNameWithPackage"/>
        <xsl:param name="className"/>
        <xsl:param name="supertypeAttr"/>
        <xsl:param name="isSupertypePresent"/>

        <xsl:variable name="inheritanceMatchesFilter">
            <xsl:call-template name="matches-package-filter">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="$supertypeAttr and $inheritanceMatchesFilter = 'true'">
            <xsl:if test="$isSupertypePresent != '' or $addSuperType = 'true'">
                <xsl:variable name="superType">
                    <xsl:call-template name="packaging-selector">
                        <xsl:with-param name="clazz" select="$supertypeAttr" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="concat($superType, ' &lt;|', $connector, ' ', $className, '&#xa;')"/>
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

            <xsl:variable name="visible">
                <xsl:call-template name="is-relationship-visible">
                    <xsl:with-param name="isTargetPresent" select="$isCompTargetPresent"/>
                    <xsl:with-param name="sourceWithPackage" select="$classNameWithPackage"/>
                    <xsl:with-param name="targetWithPackage" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="$visible = '1'">
                <xsl:variable name="target">
                    <xsl:call-template name="packaging-selector">
                        <xsl:with-param name="clazz" select="@target" />
                    </xsl:call-template>
                </xsl:variable>

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
            <xsl:variable name="target">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@target" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="isTargetPresent">
                <xsl:call-template name="is-target-present">
                    <xsl:with-param name="componentType" select="$componentType"/>
                    <xsl:with-param name="targetClassName" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="visible">
                <xsl:call-template name="is-relationship-visible">
                    <xsl:with-param name="isTargetPresent" select="$isTargetPresent"/>
                    <xsl:with-param name="sourceWithPackage" select="$classNameWithPackage"/>
                    <xsl:with-param name="targetWithPackage" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="$visible = '1'">
                <xsl:value-of select="concat($target, ' ', $dottedConnector, ' ', $className)"/>
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
            <xsl:variable name="target">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@target" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="isTargetPresent">
                <xsl:call-template name="is-target-present">
                    <xsl:with-param name="componentType" select="$componentType"/>
                    <xsl:with-param name="targetClassName" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="visible">
                <xsl:call-template name="is-relationship-visible">
                    <xsl:with-param name="isTargetPresent" select="$isTargetPresent"/>
                    <xsl:with-param name="sourceWithPackage" select="$classNameWithPackage"/>
                    <xsl:with-param name="targetWithPackage" select="$targetWithPackage"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="$visible = '1'">
                <xsl:value-of
                    select="concat($className, ' o', $connector, ' &quot;', @minCardinality, '..', @maxCardinality, '&quot; ', $target)"/>
                <xsl:call-template name="target-role-label"/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>