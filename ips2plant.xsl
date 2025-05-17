<?xml version="1.0"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="printTargetRole"/>
    <xsl:param name="addSuperType"/>
    <xsl:param name="addAssociations"/>
    <xsl:param name="addProductCmptType"/>
    <xsl:param name="showProductComponents"/>
    <xsl:param name="showTables"/>
    <xsl:param name="showEnumTypes"/>
    <xsl:param name="showEnumAssociations"/>
    <xsl:param name="showTableUsage"/>
    <xsl:param name="packages"/>
    <xsl:param name="connector"/>
    <xsl:param name="dottedConnector"/>
    <xsl:param name="packageFilter"/>

    <xsl:variable name="policySpot">(V,lightSteelBlue)</xsl:variable>
    <xsl:variable name="productSpot">(P,deepSkyBlue)</xsl:variable>
    <xsl:variable name="tableStructureSpot">(T,LightSalmon)</xsl:variable>
    <xsl:variable name="policyType">PolicyCmptType</xsl:variable>
    <xsl:variable name="productType">ProductCmptType</xsl:variable>
    <xsl:variable name="ff">&gt;&gt;</xsl:variable>
    <xsl:variable name="bb">&lt;&lt;</xsl:variable>
    <xsl:variable name="singleQuote">'</xsl:variable>

    <xsl:output method="text"/>

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- ************* POLICY OR PRODUCT COMPONENT TYPES  ************* -->
    <xsl:template match="PolicyCmptType|ProductCmptType2">
        <xsl:variable name="componentType"><xsl:value-of select="name(.)"/></xsl:variable>
        <xsl:if test="$componentType = 'PolicyCmptType' or $showProductComponents">
            <xsl:variable name="classNameWithPackage">
                <xsl:value-of select="@className"/>
            </xsl:variable>

            <xsl:variable name="className">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@className" />
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="spot">
                <xsl:choose>
                    <xsl:when test="$componentType = 'ProductCmptType2' and not(@abstract)"><xsl:value-of select="$productSpot"/></xsl:when>
                    <xsl:when test="$componentType = 'PolicyCmptType' and not(@abstract)"><xsl:value-of select="$policySpot"/></xsl:when>
                    <xsl:otherwise/>
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

            <!-- Class -->
            <xsl:if test="starts-with($classNameWithPackage, $packageFilter)">
                <xsl:if test="@abstract='true'">
                    <xsl:text>abstract </xsl:text>
                </xsl:if>
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
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
            <xsl:if test="@supertype and (starts-with($classNameWithPackage, $packageFilter))">
                <xsl:variable name="supertype">
                    <xsl:value-of select="@supertype"/>
                </xsl:variable>
                <xsl:variable name="isSupertypePresent">
                    <xsl:choose>
                        <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../PolicyCmptType[@className=$supertype]"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="../ProductCmptType2[@className=$supertype]"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <!--xsl:value-of select="concat($singleQuote, 'debug: supertype ', $supertype, ' is present: ', $isSupertypePresent, '&#xa;')"/-->
                <xsl:if test="$isSupertypePresent != '' or $addSuperType = 'true'">
                    <xsl:variable name="superType">
                        <xsl:call-template name="packaging-selector">
                            <xsl:with-param name="clazz" select="@supertype" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:value-of select="concat($superType, ' &lt;|', $connector, ' ', $className, '&#xa;')"/>
                </xsl:if>
            </xsl:if>

            <!-- Product type relation -->
            <xsl:if test="$componentType = 'PolicyCmptType' and @productCmptType and $showProductComponents = 'true' and starts-with(@productCmptType, $packageFilter)">
                <xsl:variable name="productPackaging">
                    <xsl:call-template name="packaging-selector">
                        <xsl:with-param name="clazz" select="@productCmptType" />
                    </xsl:call-template>
                </xsl:variable>

                <xsl:value-of select="concat($className, ' ', $dottedConnector, '# ', $productPackaging, '&#xa;')"/>
            </xsl:if>

            <!-- Compositions (only in PolicyCmptTypes) -->
            <xsl:for-each select="Association[@associationType='comp']">
                <xsl:variable name="targetWithPackage">
                    <xsl:value-of select="@target"/>
                </xsl:variable>
                <xsl:variable name="isCompTargetPresent">
                    <xsl:value-of select="../../PolicyCmptType[@className=$targetWithPackage]"/>
                </xsl:variable>

                <xsl:if test="($isCompTargetPresent != '' or $addAssociations = 'true')
                                    and ((starts-with($classNameWithPackage, $packageFilter) or starts-with($targetWithPackage, $packageFilter)) and $addAssociations = 'true'
                                                         or (starts-with($classNameWithPackage, $packageFilter) and starts-with($targetWithPackage, $packageFilter)))">

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
                    <xsl:if test="@targetRoleSingular and $printTargetRole = 'true'">
                        <xsl:value-of select="concat(' : ', @targetRoleSingular)"/>
                    </xsl:if>
                    <xsl:text>&#xa;</xsl:text>
                </xsl:if>
            </xsl:for-each>

            <!-- Associations -->
            <xsl:for-each select="Association[@associationType='ass']">
                <xsl:variable name="target">
                    <xsl:call-template name="packaging-selector">
                        <xsl:with-param name="clazz" select="@target" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="isAssociationTargetPresent">
                    <xsl:choose>
                        <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../../PolicyCmptType[@className=$target]"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="../../ProductCmptType2[@className=$target]"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:if test="($isAssociationTargetPresent != '' or $addAssociations = 'true')
                                    and ((starts-with($classNameWithPackage, $packageFilter) or starts-with($target, $packageFilter)) and $addAssociations = 'true'
                                                         or (starts-with($classNameWithPackage, $packageFilter) and starts-with($target, $packageFilter)))">
                    <xsl:value-of select="concat($target, ' ', $dottedConnector, ' ', $className)"/>
                    <xsl:if test="@targetRoleSingular and $printTargetRole = 'true'">
                        <xsl:value-of select="concat(' : ', @targetRoleSingular)"/>
                    </xsl:if>
                    <xsl:text>&#xa;</xsl:text>
                </xsl:if>
            </xsl:for-each>

            <!-- Aggregations -->
            <xsl:for-each select="Association[@associationType='aggr']">
                <xsl:variable name="target">
                    <xsl:call-template name="packaging-selector">
                        <xsl:with-param name="clazz" select="@target" />
                    </xsl:call-template>
                </xsl:variable>

                <xsl:variable name="isAggrTargetPresent">
                    <xsl:choose>
                        <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../../PolicyCmptType[@className=$target]"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="../../ProductCmptType2[@className=$target]"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:if test="($isAggrTargetPresent != '' or $addAssociations = 'true')
                                    and ((starts-with($classNameWithPackage, $packageFilter) or starts-with($target, $packageFilter)) and $addAssociations = 'true'
                                                         or (starts-with($classNameWithPackage, $packageFilter) and starts-with($target, $packageFilter)))">
                    <xsl:value-of
                            select="concat($className, ' o', $connector, ' &quot;', @minCardinality, '..', @maxCardinality, '&quot; ', $target)"/>
                    <xsl:if test="@targetRoleSingular and $printTargetRole = 'true'">
                        <xsl:value-of select="concat(' : ', @targetRoleSingular)"/>
                    </xsl:if>
                    <xsl:text>&#xa;</xsl:text>
                </xsl:if>
            </xsl:for-each>

            <!-- Table Structure Usage -->
            <xsl:for-each select="TableStructureUsage/TableStructure">
                <xsl:call-template name="table-usage">
                    <xsl:with-param name="tableStructure" select="@tableStructure" />
                    <xsl:with-param name="className" select="$className" />
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <!-- ************* ENUM TYPES ************* -->
    <xsl:template match="EnumType">
        <xsl:if test="$showEnumTypes">
            <xsl:variable name="classNameWithPackage">
                <xsl:value-of select="@className"/>
            </xsl:variable>

            <xsl:variable name="className">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@className" />
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="classType">
                <xsl:value-of select="$bb"/>
                <xsl:if test="not(@abstract)">
                    <xsl:text>(E,</xsl:text>
                    <xsl:choose>
                        <xsl:when test="@extensible">
                            <xsl:text>MediumSpringGreen) extensible </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>MediumSeaGreen)</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:value-of select="concat('EnumType', $ff)"/>
            </xsl:variable>

            <!-- Class -->
            <xsl:if test="starts-with($classNameWithPackage, $packageFilter)">
                <xsl:if test="@abstract='true'">
                    <xsl:text>abstract </xsl:text>
                </xsl:if>
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:for-each select="EnumAttribute">
                    <xsl:sort select="@name"/>
                    <xsl:variable name="datatype">
                        <xsl:choose>
                            <xsl:when test="@datatype = '' and @inherited = 'true'">inherited</xsl:when>
                            <xsl:otherwise><xsl:value-of select="@datatype"/></xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:value-of select="concat('  ', @name, ': ', $datatype, '&#xa;')"/>
                </xsl:for-each>

                <xsl:for-each select="EnumValue">
                    <xsl:value-of select="concat('  ',./EnumLiteralNameAttributeValue, ' (', ./EnumAttributeValue[1], ')&#xa;')"/>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>

                <xsl:for-each select="EnumAttribute">
                    <xsl:call-template name="enum-association">
                        <xsl:with-param name="enumType" select="@datatype" />
                        <xsl:with-param name="className" select="$className" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- Inheritance -->
            <xsl:if test="@superEnumType and (starts-with($classNameWithPackage, $packageFilter))">
                <xsl:variable name="supertypeWithPackage">
                    <xsl:value-of select="@superEnumType"/>
                </xsl:variable>
                <xsl:variable name="isSupertypePresent">
                    <xsl:value-of select="../EnumType[@className=$supertypeWithPackage]"/>
                </xsl:variable>
                <!--xsl:value-of select="concat($singleQuote, 'debug: supertype ', $supertype, ' is present: ', $isSupertypePresent, '&#xa;')"/-->
                <xsl:if test="$isSupertypePresent != '' or $addSuperType = 'true'">
                    <xsl:variable name="superType">
                        <xsl:call-template name="packaging-selector">
                            <xsl:with-param name="clazz" select="@superEnumType" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:value-of select="concat($superType, ' &lt;|', $connector, ' ', $className, '&#xa;')"/>
                </xsl:if>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- ************* TABLES ************* -->
    <xsl:template match="TableStructure">
        <xsl:if test="$showTables">
            <xsl:variable name="classNameWithPackage">
                <xsl:value-of select="@className"/>
            </xsl:variable>

            <xsl:variable name="className">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@className" />
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="classType">
                <xsl:value-of select="concat($bb, $tableStructureSpot, 'TableStructure', $ff)"/>
            </xsl:variable>

            <!-- Class -->
            <xsl:if test="starts-with($classNameWithPackage, $packageFilter)">
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:for-each select="Column">
                    <xsl:sort select="@name"/>
                    <xsl:variable name="datatype">
                        <xsl:call-template name="substring-after-last">
                            <xsl:with-param name="string" select="@datatype" />
                            <xsl:with-param name="delimiter" select="'.'" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:value-of select="concat('  ', @name, ': ', $datatype, '&#xa;')"/>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>

                <!-- Datatype associations -->
                <xsl:for-each select="Column">
                    <xsl:call-template name="enum-association">
                        <xsl:with-param name="enumType" select="@datatype" />
                        <xsl:with-param name="className" select="$className" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- ************* HELPERS ************* -->
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
        <xsl:if test="starts-with($className, $packageFilter) and $showTableUsage">
            <xsl:value-of select="concat($className, ' ', $dottedConnector, '{ ', $tableStructurePackaging, '&#xa;')"/>
        </xsl:if>
    </xsl:template>

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

    <!-- ignore -->
    <xsl:template match="EnumContent|ProductCmpt|ProductVariant|TableContents">
    </xsl:template>

</xsl:stylesheet>