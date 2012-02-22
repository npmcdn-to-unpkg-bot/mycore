<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="xlink mcr i18n acl mods mcrmods" version="1.0">
  <xsl:param name="MCR.Handle.Resolver.MasterURL" />
  <xsl:param name="MCR.Users.Guestuser.UserName" />

  <xsl:template name="printMetaDate.mods">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="i18n:translate(concat('metaData.mods.dictionary.',local-name($nodes[1])))" />
    <xsl:param name="sep" select="''" />
    <xsl:message>
      <xsl:value-of select="concat('label: ',$label)" />
    </xsl:message>
    <xsl:message>
      <xsl:value-of select="concat('nodes: ',count($nodes))" />
    </xsl:message>
    <xsl:if test="$nodes">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat($label,':')" />
        </td>
        <td class="metavalue">
          <xsl:variable name="selectPresentLang">
            <xsl:call-template name="selectPresentLang">
              <xsl:with-param name="nodes" select="$nodes" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:for-each select="$nodes">
            <xsl:if test="position()!=1">
              <xsl:choose>
                <xsl:when test="string-length($sep)&gt;0">
                  <xsl:value-of select="$sep" />
                </xsl:when>
                <xsl:otherwise>
                  <br />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
            <xsl:if test="not(@xml:lang) or @xml:lang=$selectPresentLang">
              <xsl:call-template name="lf2br">
                <xsl:with-param name="string" select="normalize-space(.)"/>
              </xsl:call-template>
            </xsl:if>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateOther|mods:dateIssued" mode="present">
    <xsl:param name="label" select="i18n:translate(concat('metaData.mods.dictionary.',local-name()))" />
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat($label,':')" />
        </td>
        <td class="metavalue">
          <xsl:apply-templates select="." mode="formatDate"/>
        </td>
      </tr>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateOther|mods:dateIssued" mode="formatDate">
    <xsl:variable name="formatted">
      <xsl:call-template name="formatISODate">
        <xsl:with-param name="date" select="." />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="string-length($formatted)&gt;2 
                      and starts-with($formatted, '?')
                      and substring($formatted,string-length($formatted),1)='?'">
        <xsl:value-of select="translate($formatted, '?', '')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$formatted" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="category" mode="printModsClassInfo">
    <xsl:variable name="categurl">
      <xsl:if test="url">
        <xsl:choose>
            <!-- MCRObjectID should not contain a ':' so it must be an external link then -->
          <xsl:when test="contains(url/@xlink:href,':')">
            <xsl:value-of select="url/@xlink:href" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',url/@xlink:href,$HttpSession)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="selectLang">
      <xsl:call-template name="selectLang">
        <xsl:with-param name="nodes" select="./label" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="./label[lang($selectLang)]">
      <xsl:choose>
        <xsl:when test="string-length($categurl) != 0">
          <a href="{$categurl}">
            <xsl:if test="$wcms.useTargets = 'yes'">
              <xsl:attribute name="target">_blank</xsl:attribute>
            </xsl:if>
            <xsl:value-of select="@text" />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@text" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="*" mode="printModsClassInfo">
    <xsl:variable name="classlink" select="mcrmods:getClassCategLink(.)" />
    <xsl:choose>
      <xsl:when test="string-length($classlink) &gt; 0">
        <xsl:for-each select="document($classlink)/mycoreclass/categories/category">
          <xsl:apply-templates select="." mode="printModsClassInfo" />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="@valueURI">
            <xsl:apply-templates select="." mode="hrefLink" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="text()" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*[@valueURI]" mode="hrefLink">
    <a href="{@valueURI}">
      <xsl:choose>
        <xsl:when test="mods:displayForm">
          <xsl:value-of select="mods:displayForm" />
        </xsl:when>
        <xsl:when test="@displayLabel">
          <xsl:value-of select="@displayLabel" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@valueURI" />
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="mods:titleInfo" mode="present">
    <xsl:if test="not(@transliteration='text/html')">
      <xsl:for-each select="mods:title">
        <tr>
          <td valign="top" class="metaname">
            <xsl:choose>
              <xsl:when test="./../@type='translated'">
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.title'),' (',./../@xml:lang,') :')" />
              </xsl:when>
              <xsl:when test="./../@type='alternative' and ./../@displayLabel='Short form of the title'">
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.shorttitle'),' :')" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.title'),' :')" />
              </xsl:otherwise>
            </xsl:choose>
          </td>
          <td class="metavalue">
            <xsl:choose>
              <xsl:when test="not(./../@type='translated' or ./../@type='alternative') and //mods:titleInfo[@transliteration='text/html']">
                <xsl:value-of select="//mods:titleInfo[@transliteration='text/html']/mods:title" disable-output-escaping="yes" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="lf2br">
                  <xsl:with-param name="string" select="."/>
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>
      </xsl:for-each>
      <xsl:if test="mods:subTitle">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.subtitle'),':')" />
          </td>
          <td class="metavalue subTitle">
            <xsl:value-of select="mods:subTitle" />
          </td>
        </tr>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.titleContent">
    <xsl:variable name="modsType">
      <xsl:choose>
        <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
          <xsl:value-of select="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']/@valueURI,'#')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates mode="mods-type" select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <div id="title_content" class="block_content">
      <div class="subcolumns">
        <div class="c85l">
          <table class="metaData">
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID)]" />
          </table>
        </div>
        <div class="c15r">
          <xsl:if test="./structure/derobjects">
            <xsl:variable name="objectBaseURL">
              <xsl:if test="$objectHost != 'local'">
                <xsl:value-of select="document('webapp:hosts.xml')/mcr:hosts/mcr:host[@alias=$objectHost]/mcr:url[@type='object']/@href" />
              </xsl:if>
              <xsl:if test="$objectHost = 'local'">
                <xsl:value-of select="concat($WebApplicationBaseURL,'receive/')" />
              </xsl:if>
            </xsl:variable>
            <xsl:variable name="staticURL">
              <xsl:value-of select="concat($objectBaseURL,@ID)" />
            </xsl:variable>
            <xsl:apply-templates mode="printDerivatesThumb" select=".">
              <xsl:with-param select="$staticURL" name="staticURL" />
              <xsl:with-param select="$modsType" name="modsType" />
            </xsl:apply-templates>
          </xsl:if>
        </div>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="mods:abstract" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.abstract'),' (' ,@xml:lang,') :')" />
      </td>
      <td class="metavalue">
        <xsl:call-template name="lf2br">
          <xsl:with-param name="string" select="."/>
        </xsl:call-template>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.abstractContent">
    <div id="abstract_box" class="detailbox">
      <h4 id="abstract_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.abstractbox')" />
      </h4>
      <div id="abstract_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="mods:extent" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.extent'),':')" />
      </td>
      <td class="metavalue">
        <xsl:call-template name="printMetaDate.mods.extent" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.extent">
    <xsl:choose>
      <xsl:when test="count(mods:start) &gt; 0">
        <xsl:choose>
          <xsl:when test="count(mods:end) &gt; 0">
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.page.abbr'),' ',mods:start,'-',mods:end)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.page.abbr'),' ',mods:start)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat(mods:total,' ',i18n:translate('metaData.mods.dictionary.pages'))" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:extension[@displayLabel='characteristics']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.characteristics'),':')" />
      </td>
      <td class="metavalue">
        <table>
          <tr>
            <th>
              <xsl:value-of select="i18n:translate('metaData.mods.dictionary.year')" />
            </th>
            <th>
              <xsl:value-of select="i18n:translate('metaData.mods.dictionary.impact')" />
            </th>
            <th>
              <xsl:value-of select="i18n:translate('metaData.mods.dictionary.refereed')" />
            </th>
          </tr>
          <xsl:for-each select="chars">
            <tr>
              <td>
                <xsl:value-of select="@year" />
              </td>
              <td>
                <xsl:value-of select="@factor" />
              </td>
              <td>
                <xsl:value-of select="i18n:translate(concat('metaData.mods.dictionary.refereed.',@refereed))" />
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:extension" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate(concat('metaData.mods.dictionary.',@displayLabel)),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:name" mode="printName">
    <xsl:choose>
      <xsl:when test="@valueURI">
        <!-- derived from printModsClassInfo template -->
        <xsl:variable name="classlink" select="mcrmods:getClassCategParentLink(.)" />
        <xsl:choose>
          <xsl:when test="string-length($classlink) &gt; 0">
            <xsl:for-each select="document($classlink)/mycoreclass//category[position()=1 or position()=last()]">
              <xsl:if test="position() > 1">
                <xsl:value-of select="', '" />
              </xsl:if>
              <xsl:apply-templates select="." mode="printModsClassInfo" />
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="." mode="hrefLink" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="mods:namePart">
        <xsl:choose>
          <xsl:when test="mods:namePart[@type='given'] and mods:namePart[@type='family']">
            <xsl:value-of select="concat(mods:namePart[@type='family'], ', ',mods:namePart[@type='given'])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="mods:namePart" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="mods:displayForm">
        <xsl:value-of select="mods:displayForm" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:name" mode="present"><!-- ToDo: all authors, rev ... in one column -->
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']">
            <xsl:apply-templates select="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']" mode="printModsClassInfo" />
            <xsl:value-of select="':'" />
          </xsl:when>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator']">
            <xsl:value-of
              select="concat(i18n:translate(concat('metaData.mods.dictionary.',mods:role/mods:roleTerm[@authority='marcrelator'])),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.name'),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printName" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:name[@type='corporate' and @ID]" mode="present">
    <xsl:variable name="id" select="concat('#', @ID)" />
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.institution.label'),':')" />
      </td>
      <td class="metavalue"><!-- ToDo: Ausgabe der Einrichtung mit jeweils oberster Einrichtung (Max Rubner-Institut, Institut für ...) -->
        <xsl:apply-templates select="." mode="printName" />
      </td>
    </tr>
    <xsl:if
      test="($CurrentUser!=$MCR.Users.Guestuser.UserName and ./../mods:note[@xlink:href=$id]) or (./../mods:location/mods:physicalLocation[@xlink:href=$id])">
      <tr>
        <td colspan="2">
          <table class="metaData">
            <xsl:if test="$CurrentUser!=$MCR.Users.Guestuser.UserName">
              <xsl:call-template name="printMetaDate.mods">
                <xsl:with-param name="nodes" select="./../mods:note[@xlink:href=$id]" />
              </xsl:call-template>
            </xsl:if>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./../mods:location/mods:physicalLocation[@xlink:href=$id]" />
            </xsl:call-template>
          </table>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='hdl']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="'Handle:'" />
      </td>
      <td class="metavalue">
        <xsl:variable name="hdl" select="." />
        <a href="{$MCR.Handle.Resolver.MasterURL}{$hdl}">
          <xsl:value-of select="$hdl" />
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="i18n:translate(concat('metaData.mods.dictionary.identifier.',@type))" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='uri' or type='doi']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="contains(.,'PPN=')">
            <xsl:value-of select="i18n:translate('metaData.mods.dictionary.identifier.ppn')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="i18n:translate(concat('metaData.mods.dictionary.identifier.',@type))" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:variable name="link" select="." />
        <xsl:choose>
          <xsl:when test="contains(.,'PPN=')">
            <a href="{$link}">
              <xsl:value-of select="substring-after($link, 'PPN=')" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <a href="{$link}">
              <xsl:value-of select="$link" />
            </a>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:classification" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="@authority='sdnb'">
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.sdnb'), ':')" />
          </xsl:when>
          <xsl:when test="@displayLabel='annual_review'">
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.annual_review'), ' (')" />
            <xsl:value-of select="concat(@edition, ') :')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.classification'), ':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printModsClassInfo" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:language" mode="present">
    <xsl:param name="sep" select="''" />
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.language'), ':')" />
      </td>
      <td class="metavalue">
        <xsl:for-each select="mods:languageTerm">
          <xsl:if test="position()!=1">
            <xsl:choose>
              <xsl:when test="string-length($sep)&gt;0">
                <xsl:value-of select="$sep" />
              </xsl:when>
              <xsl:otherwise>
                <br />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:apply-templates select="." mode="printModsClassInfo" />
        </xsl:for-each>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:url" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.url'),':')" />
      </td>
      <td class="metavalue">
        <a>
          <xsl:attribute name="href"><xsl:value-of select="." /></xsl:attribute>
          <xsl:value-of select="@displayLabel" />
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="present"><!-- ToDo: show cc icon and more information ... -->
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.accessCondition'),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.permalink">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.permalink'),':')" />
      </td>
      <td class="metavalue">
        <a href="http://openagrar.bmelv-forschung.de/receive/{@ID}">Permalink</a>
<!--         <xsl:text> | </xsl:text>
        <xsl:call-template name="shareButton">
          <xsl:with-param name="linkURL" select="concat($ServletsBaseURL,'receive/',@ID)" />
          <xsl:with-param name="linkTitle" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
        </xsl:call-template> -->
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.categoryContent">
    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateCreated) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='submitted']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='accepted']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.genre.kindof'),':')" />
                </td>
                <td class="metavalue">
                  <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
                    mode="printModsClassInfo" />
                </td>
              </tr>
            </xsl:if>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
            </xsl:call-template>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateCreated" mode="present"/>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='submitted']" mode="present">
              <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.dateSubmitted')" />
            </xsl:apply-templates>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='accepted']" mode="present">
              <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.dateAccepted')" />
            </xsl:apply-templates>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" mode="present"/>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>


  <!-- view report metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.report">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.report')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>
  <!-- END: view report metadata -->

  <!-- view thesis metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.thesis">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.thesis'), ' - ')" />
        <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
          mode="printModsClassInfo" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>
  <!-- END: view thesis metadata -->

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="title.cproceeding">
    <xsl:choose>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
        <xsl:call-template name="ShortenText">
          <xsl:with-param name="text" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
          <xsl:with-param name="length" select="70" />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
        <xsl:variable name="completeTitle">
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
            <xsl:for-each select="mods:namePart[not(@type)]">
              <xsl:choose>
                <xsl:when test="position()=1">
                  <xsl:value-of select="." />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(' – ',.)" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
            <xsl:if test="mods:namePart[@type='date']">
              <xsl:value-of select="', '" />
              <xsl:value-of select="mods:namePart[@type='date']" />
            </xsl:if>
            <xsl:for-each select="mods:affiliation">
              <xsl:value-of select="concat(', ',.)" />
            </xsl:for-each>
          </xsl:for-each>
        </xsl:variable>
        <xsl:call-template name="ShortenText">
          <xsl:with-param name="text" select="i18n:translate('metaData.mods.dictionary.proceedingOf',$completeTitle)" />
          <xsl:with-param name="length" select="70" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@ID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- view conference proceeding metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.cproceeding">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.confpro')" />
      </h4>
      <div id="title_content" class="block_content">
        <div class="subcolumns">
          <div class="c85l">
            <table class="metaData">
              <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.conference.title'),':')" />
                </td>
                <td class="metavalue">
                  <strong>
                    <xsl:for-each select="mods:namePart[not(@type)]">
                      <xsl:choose>
                        <xsl:when test="position()=1">
                          <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:otherwise>
                          <em>
                            <xsl:value-of select="concat(' – ',.)" />
                          </em>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:for-each>
                  </strong>
                  <xsl:if test="mods:namePart[@type='date']">
                    <em>
                      <xsl:value-of select="', '" />
                      <xsl:value-of select="mods:namePart[@type='date']" />
                    </em>
                  </xsl:if>
                  <xsl:for-each select="mods:affiliation">
                    <xsl:value-of select="concat(', ',.)" />
                  </xsl:for-each>
                </td>
              </xsl:for-each>
              <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
            </table>
          </div>
          <div class="c15r">
            <xsl:if test="./structure/derobjects">
              <xsl:variable name="objectBaseURL">
                <xsl:if test="$objectHost != 'local'">
                  <xsl:value-of select="document('webapp:hosts.xml')/mcr:hosts/mcr:host[@alias=$objectHost]/mcr:url[@type='object']/@href" />
                </xsl:if>
                <xsl:if test="$objectHost = 'local'">
                  <xsl:value-of select="concat($WebApplicationBaseURL,'receive/')" />
                </xsl:if>
              </xsl:variable>
              <xsl:variable name="staticURL">
                <xsl:value-of select="concat($objectBaseURL,@ID)" />
              </xsl:variable>
              <xsl:apply-templates mode="printDerivatesThumb" select=".">
                <xsl:with-param select="$staticURL" name="staticURL" />
                <xsl:with-param select="'confpro'" name="modsType" />
              </xsl:apply-templates>
            </xsl:if>
          </div>
        </div>
      </div>
    </div>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@type='conference')]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@type='conference')]" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view conference proceeding metadata -->

  <!-- view conference publication metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.cpublication">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.confpub')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID)]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo[@type='isReferencedBy']/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <xsl:variable name="parentID" select="./structure/parents/parent/@xlink:href" />
          <table class="metaData">
            <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.confpubIn'),':')" />
                </td>
                <td class="metavalue">
                  <!-- Conference -->
                  <xsl:choose>
                    <xsl:when test="string-length($parentID)!=0">
                      <xsl:call-template name="objectLink">
                        <xsl:with-param select="$parentID" name="obj_id" />
                      </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="mods:titleInfo/mods:title" />
                    </xsl:otherwise>
                  </xsl:choose>
                  <xsl:text disable-output-escaping="yes">&lt;br /></xsl:text>
                  <xsl:for-each select="mods:part/mods:extent[@unit='pages']">
                    <xsl:call-template name="printMetaDate.mods.extent" />
                  </xsl:for-each>
                </td>
              </tr>
            </xsl:for-each>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo[@type='isReferencedBy']/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.authority')" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view conference publication metadata -->

  <!-- view book metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.book">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.book')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:edition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:detail/mods:number) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:edition" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:detail/mods:number" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view book metadata -->

  <!-- view book chapter metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.book-chapter">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.chapter')" />
      </h4>
<!--  <xsl:call-template name="printMetaDate.mods">
        <xsl:with-param name="nodes"
          select="./metadata/def.modsContainer/modsContainer/mods:mods/modsrelatedItem[not(@type='isReferencedBy')]/mods:titleInfo/mods:title" />
      </xsl:call-template> -->
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID)]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo[@type='isReferencedBy']/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <xsl:variable name="parentID" select="./structure/parents/parent/@xlink:href" />
          <table class="metaData">
            <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.chapterIn'),':')" />
                </td>
                <td class="metavalue">
                  <!-- Book -->
                  <xsl:choose>
                    <xsl:when test="string-length($parentID)!=0">
                      <xsl:call-template name="objectLink">
                        <xsl:with-param select="$parentID" name="obj_id" />
                      </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="mods:titleInfo/mods:title" />
                    </xsl:otherwise>
                  </xsl:choose>
                  <xsl:text disable-output-escaping="yes">&lt;br /></xsl:text>
                  <xsl:for-each select="mods:part/mods:extent[@unit='pages']">
                    <xsl:call-template name="printMetaDate.mods.extent" />
                  </xsl:for-each>
                </td>
              </tr>
            </xsl:for-each>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo[@type='isReferencedBy']/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.authority')" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>

  </xsl:template>
  <!-- END: view book chapter metadata -->

  <!-- view journal metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.journal">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.journal')" />
      </h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />

          <xsl:if test="./structure/children/child">
            <!--*** List children per object type ************************************* -->
            <!-- 1.) get a list of objectTypes of all child elements 2.) remove duplicates from this list 3.) for-each objectTyp id list child elements -->
            <xsl:variable name="objectTypes">
              <xsl:for-each select="./structure/children/child/@xlink:href">
                <id>
                  <xsl:copy-of select="substring-before(substring-after(.,'_'),'_')" />
                </id>
              </xsl:for-each>
            </xsl:variable>
            <xsl:variable select="xalan:nodeset($objectTypes)/id[not(.=following::id)]" name="unique-ids" />
            <!-- the for-each would iterate over <id> with root not beeing /mycoreobject so we save the current node in variable context to access 
              needed nodes -->
            <xsl:variable select="." name="context" />
            <xsl:for-each select="$unique-ids">
              <xsl:variable select="." name="thisObjectType" />
              <xsl:variable name="label">
                <xsl:value-of select="'enthält'" />
  <!--          <xsl:choose>
                  <xsl:when test="count($context/structure/children/child[contains(@xlink:href,$thisObjectType)])=1">
                    <xsl:value-of select="i18n:translate(concat('metaData.',$thisObjectType,'.[singular]'))" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="i18n:translate(concat('metaData.',$thisObjectType,'.[plural]'))" />
                  </xsl:otherwise>
                </xsl:choose> -->
              </xsl:variable>
              <xsl:call-template name="printMetaDate">
                <xsl:with-param select="$context/structure/children/child[contains(@xlink:href, concat('_',$thisObjectType,'_'))]"
                  name="nodes" />
                <xsl:with-param select="$label" name="label" />
              </xsl:call-template>
            </xsl:for-each>
          </xsl:if>

          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods.permalink" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:extension" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>
  <!-- END: view journal metadata -->

  <!-- view series metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.series">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.series')" />
      </h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />

          <xsl:if test="./structure/children/child">
            <!--*** List children per object type ************************************* -->
            <!-- 1.) get a list of objectTypes of all child elements 2.) remove duplicates from this list 3.) for-each objectTyp id list child elements -->
            <xsl:variable name="objectTypes">
              <xsl:for-each select="./structure/children/child/@xlink:href">
                <id>
                  <xsl:copy-of select="substring-before(substring-after(.,'_'),'_')" />
                </id>
              </xsl:for-each>
            </xsl:variable>
            <xsl:variable select="xalan:nodeset($objectTypes)/id[not(.=following::id)]" name="unique-ids" />
            <!-- the for-each would iterate over <id> with root not beeing /mycoreobject so we save the current node in variable context to access 
              needed nodes -->
            <xsl:variable select="." name="context" />
            <xsl:for-each select="$unique-ids">
              <xsl:variable select="." name="thisObjectType" />
              <xsl:variable name="label">
                <xsl:value-of select="'enthält'" />
  <!--          <xsl:choose>
                  <xsl:when test="count($context/structure/children/child[contains(@xlink:href,$thisObjectType)])=1">
                    <xsl:value-of select="i18n:translate(concat('metaData.',$thisObjectType,'.[singular]'))" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="i18n:translate(concat('metaData.',$thisObjectType,'.[plural]'))" />
                  </xsl:otherwise>
                </xsl:choose> -->
              </xsl:variable>
              <xsl:call-template name="printMetaDate">
                <xsl:with-param select="$context/structure/children/child[contains(@xlink:href, concat('_',$thisObjectType,'_'))]"
                  name="nodes" />
                <xsl:with-param select="$label" name="label" />
              </xsl:call-template>
            </xsl:for-each>
          </xsl:if>

          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods.permalink" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:extension" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
          </xsl:call-template>
        </table>
      </div>
    </div>

  </xsl:template>
  <!-- END: view series metadata -->

  <!-- view article metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.article">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.article')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <xsl:variable name="parentID" select="./structure/parents/parent/@xlink:href" />
          <table class="metaData">
            <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.articleIn'),':')" />
                </td>
                <td class="metavalue">
                  <!-- Journal -->
                  <xsl:choose>
                    <xsl:when test="string-length($parentID)!=0">
                      <xsl:call-template name="objectLink">
                        <xsl:with-param select="$parentID" name="obj_id" />
                      </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="mods:titleInfo/mods:title" />
                    </xsl:otherwise>
                  </xsl:choose>
                  <xsl:text disable-output-escaping="yes">&lt;br /></xsl:text>
                  <!-- Issue -->
                  <xsl:value-of
                    select="concat(mods:part/mods:detail[@type='issue']/mods:caption,' ',mods:part/mods:detail[@type='issue']/mods:number)" />
                  <xsl:if test="mods:part/mods:date">
                    <xsl:value-of select="concat('/',mods:part/mods:date,' ')" />
                  </xsl:if>
                  <!-- Volume -->
                  <xsl:if test="mods:part/mods:detail[@type='volume']/mods:number">
                    <xsl:value-of
                      select="concat('(',i18n:translate('metaData.mods.dictionary.volume.article'),': ',mods:part/mods:detail[@type='volume']/mods:number,')')" />
                  </xsl:if>
                  <!-- Pages -->
                  <xsl:for-each select="mods:part/mods:extent[@unit='pages']">
                    <xsl:call-template name="printMetaDate.mods.extent" />
                  </xsl:for-each>
                  <!-- date issued -->
                </td>
              </tr>
            </xsl:for-each>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:originInfo/mods:dateIssued" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.2ndSource')" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view article metadata -->

  <!-- view av nedia metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.av-media">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.av')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />

  </xsl:template>
  <!-- END: view av nedia metadata -->

</xsl:stylesheet>