<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <!-- Policy or Product component types -->
    <xsl:template match="PolicyCmptType|ProductCmptType2">
        <xsl:variable name="componentType" select="name(.)"/>
        <xsl:if test="$componentType = 'PolicyCmptType' or $showProductComponents">
            <xsl:variable name="classNameWithPackage" select="@className"/>
            <xsl:variable name="className" select="f:class-name(@className)"/>

            <xsl:variable name="spot">
                <xsl:choose>
                    <xsl:when test="$componentType = 'ProductCmptType2' and not(@abstract)"><xsl:value-of select="$productSpot"/></xsl:when>
                    <xsl:when test="$componentType = 'PolicyCmptType' and not(@abstract)"><xsl:value-of select="$policySpot"/></xsl:when>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="classType">
                <xsl:choose>
                    <xsl:when test="$componentType = 'ProductCmptType2'">
                        <xsl:value-of select="concat($bb, $spot, $productType, $ff)"/>
                    </xsl:when>
                    <xsl:when test="$componentType = 'PolicyCmptType'">
                        <xsl:value-of select="concat($bb, $spot, $policyType, $ff)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat($bb, name(.), $ff)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <!-- Class definition -->
            <xsl:if test="f:matches-package-filter($classNameWithPackage)">
                <xsl:if test="@abstract='true'">
                    <xsl:text>abstract </xsl:text>
                </xsl:if>
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:if test="$showMavenModule = 'true' and @mavenModule">
                    <xsl:value-of select="concat('  ', @mavenModule, '&#xa;')"/>
                    <xsl:text>  --&#xa;</xsl:text>
                </xsl:if>
                <xsl:for-each select="Attribute">
                    <xsl:sort select="@name"/>
                    <xsl:variable name="attrType">
                        <xsl:choose>
                            <xsl:when test="@attributeType='changeable'">+</xsl:when>
                            <xsl:when test="@attributeType='derived'">~</xsl:when>
                            <xsl:when test="@attributeType='computed'">#</xsl:when>
                            <xsl:when test="@attributeType='constant'">-</xsl:when>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:value-of select="concat('  ', $attrType, @name, ': ', @datatype, '&#xa;')"/>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>
                <xsl:for-each select="Attribute">
                    <xsl:call-template name="enum-association">
                        <xsl:with-param name="enumType" select="@datatype" />
                        <xsl:with-param name="className" select="$className" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- Inheritance -->
            <xsl:variable name="isSupertypePresent">
                <xsl:choose>
                    <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../PolicyCmptType[@className=current()/@supertype]"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="../ProductCmptType2[@className=current()/@supertype]"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="inheritance">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
                <xsl:with-param name="supertypeAttr" select="@supertype"/>
                <xsl:with-param name="isSupertypePresent" select="$isSupertypePresent"/>
            </xsl:call-template>

            <!-- Product type relation -->
            <xsl:if test="$componentType = 'PolicyCmptType' and @productCmptType and $showProductComponents = 'true' and f:matches-package-filter(@productCmptType)">
                <xsl:value-of select="concat($className, ' ', $dottedConnector, '# ', f:class-name(@productCmptType), '&#xa;')"/>
            </xsl:if>

            <!-- Compositions, associations, aggregations -->
            <xsl:call-template name="compositions">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
            </xsl:call-template>

            <xsl:call-template name="associations">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
                <xsl:with-param name="componentType" select="$componentType"/>
            </xsl:call-template>

            <xsl:call-template name="aggregations">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
                <xsl:with-param name="componentType" select="$componentType"/>
            </xsl:call-template>

            <!-- Table structure usage -->
            <xsl:for-each select="TableStructureUsage/TableStructure">
                <xsl:call-template name="table-usage">
                    <xsl:with-param name="tableStructure" select="@tableStructure" />
                    <xsl:with-param name="className" select="$className" />
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
