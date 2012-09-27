<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="xalan xlink mcr i18n acl mods mcrxsl encoder" version="1.0">
  <xsl:param select="'local'" name="objectHost" />
  <xsl:param name="MCR.Users.Superuser.UserName" />
  <xsl:include href="mods2html.xsl" />
  <xsl:include href="modsmetadata.xsl" />
  <xsl:include href="mods-highwire.xsl" />

  <xsl:include href="basket.xsl" />

  <xsl:include href="modshitlist-external.xsl" />  <!-- for external usage in application module -->
  <xsl:include href="modsdetails-external.xsl" />  <!-- for external usage in application module -->

  <xsl:variable name="head.additional">
    <xsl:if test="contains(/mycoreobject/@ID,'_mods_')">
      <!-- ==================== Highwire Press tags ==================== -->
      <xsl:apply-templates select="/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods" mode="highwire" />
    </xsl:if>
  </xsl:variable>

  <!--Template for result list hit: see results.xsl -->
  <xsl:template match="mcr:hit[contains(@id,'_mods_')]" priority="1">
    <xsl:param name="mcrobj" />
    <xsl:param name="mcrobjlink" />
    <xsl:variable select="100" name="DESCRIPTION_LENGTH" />
    <xsl:variable select="@host" name="host" />
    <xsl:variable name="obj_id">
      <xsl:value-of select="@id" />
    </xsl:variable>
    <tr>
      <td colspan="2" class="resultTitle">
        <xsl:call-template name="objectLink">
          <xsl:with-param select="$mcrobj" name="mcrobj" />
        </xsl:call-template>
      </td>
    </tr>
    <tr>
      <td colspan="2" class="description">
        <div>
          <xsl:for-each select="$mcrobj/metadata/def.modsContainer/modsContainer/*">    <!-- Title, 16pt -->
            <xsl:for-each select="mods:titleInfo/mods:title">
              <xsl:value-of select="." />
              <br />
            </xsl:for-each>
    <!-- Link to presentation, ?pt -->
            <xsl:for-each select="mods:identifier[@type='uri']">
              <a href="{.}">
                <xsl:value-of select="." />
              </a>
              <br />
            </xsl:for-each>
    <!-- Place, ?pt -->
            <xsl:for-each select="mods:originInfo/mods:place/mods:placeTerm[@type='text']">
              <xsl:value-of select="." />
            </xsl:for-each>
    <!-- Author -->
            <xsl:for-each select="mods:name[mods:role/mods:roleTerm/text()='aut']">
              <xsl:if test="position()!=1">
                <xsl:value-of select="'; '" />
              </xsl:if>
              <xsl:apply-templates select="." mode="printName" />
              <xsl:if test="position()=last()">
                <br />
              </xsl:if>
            </xsl:for-each>
    <!-- Shelfmark -->
            <xsl:for-each select="mods:location/mods:shelfLocator">
              <xsl:value-of select="." />
              <br />
            </xsl:for-each>
    <!-- URN -->
            <xsl:for-each select="mods:identifier[@type='urn']">
              <xsl:value-of select="." />
              <br />
            </xsl:for-each>
          </xsl:for-each>
        </div>
        <!-- you could insert here a preview for your metadata, e.g. uncomment the next block and replace "your-tags/here" by something of your 
          metadata -->
        <!-- <div> short description: <xsl:call-template name="printI18N"> <xsl:with-param name="nodes" select="$mcrobj/metadata/your-tags/here" 
          /> </xsl:call-template> </div> -->
        <span class="properties">
          <xsl:variable name="date">
            <xsl:call-template name="formatISODate">
              <xsl:with-param select="$mcrobj/service/servdates/servdate[@type='modifydate']" name="date" />
              <xsl:with-param select="i18n:translate('metaData.date')" name="format" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:value-of select="i18n:translate('results.lastChanged',$date)" />
        </span>
      </td>
    </tr>
  </xsl:template>
  
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="basketContent">
    <xsl:call-template name="objectLink">
      <xsl:with-param select="." name="mcrobj" />
    </xsl:call-template>
    <div class="description">
      <xsl:for-each select="./metadata/def.modsContainer/modsContainer/*">
<!-- Link to presentation, ?pt -->
        <xsl:for-each select="mods:identifier[@type='uri']">
          <a href="{.}">
            <xsl:value-of select="." />
          </a>
          <br />
        </xsl:for-each>
<!-- Place, ?pt -->
        <xsl:for-each select="mods:originInfo/mods:place/mods:placeTerm[@type='text']">
          <xsl:value-of select="." />
        </xsl:for-each>
<!-- Author -->
        <xsl:for-each select="mods:name[mods:role/mods:roleTerm/text()='aut']">
          <xsl:if test="position()!=1">
            <xsl:value-of select="'; '" />
          </xsl:if>
          <xsl:apply-templates select="." mode="printName" />
          <xsl:if test="position()=last()">
            <br />
          </xsl:if>
        </xsl:for-each>
<!-- Shelfmark -->
        <xsl:for-each select="mods:location/mods:shelfLocator">
          <xsl:value-of select="." />
          <br />
        </xsl:for-each>
<!-- URN -->
        <xsl:for-each select="mods:identifier[@type='urn']">
          <xsl:value-of select="." />
          <br />
        </xsl:for-each>
      </xsl:for-each>
    </div>
  </xsl:template>

  <!--Template for generated link names and result titles: see mycoreobject.xsl, results.xsl, MyCoReLayout.xsl -->
  <xsl:template priority="1" mode="resulttitle" match="/mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods-type" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$mods-type='confpro'">
        <xsl:apply-templates select="." mode="title.confpro" />
      </xsl:when>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
        <xsl:call-template name="ShortenText">
          <xsl:with-param name="text" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
          <xsl:with-param name="length" select="70" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@ID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--Template for title in metadata view: see mycoreobject.xsl -->
  <xsl:template priority="1" mode="title" match="/mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods-type" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$mods-type='confpro'">
        <xsl:apply-templates select="." mode="title.confpro" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
            <xsl:variable name="text">
              <xsl:choose>
                <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@transliteration]/mods:title">
                  <!-- TODO: if editor bug fixed -->
                  <xsl:value-of select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@transliteration]/mods:title" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:value-of select="$text" disable-output-escaping="yes" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@ID" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods-type" match="/mycoreobject">
    <xsl:choose>
      <xsl:when
        test="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='intern']/@valueURI,'#')='article' or
                      (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:genre='periodical' and
                       ./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier/@type='doi')">
        <xsl:value-of select="'article'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='intern']/@valueURI,'#')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--Template for metadata view: see mycoreobject.xsl -->
  <xsl:template priority="1" mode="present" match="/mycoreobject[contains(@ID,'_mods_')]">
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

      <!--1***modsContainer************************************* -->
    <xsl:variable name="mods-type">
      <xsl:apply-templates mode="mods-type" select="." />
    </xsl:variable>
    <xsl:message>
      MODS-TYPE:
      <xsl:value-of select="$mods-type" />
    </xsl:message>

    <div id="detail_view" class="blockbox">
      <h3>
        <xsl:apply-templates select="." mode="resulttitle" /><!-- shorten plain text title (without html) -->
      </h3>
      <xsl:choose>
        <xsl:when test="$mods-type='series'">
          <xsl:call-template name="mods.editobject_without_table">
            <xsl:with-param select="./@ID" name="id" />
            <xsl:with-param select="'journal'" name="layout" />
            <xsl:with-param select="$mods-type" name="mods-type" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="mods.editobject_without_table">
            <xsl:with-param select="./@ID" name="id" />
            <xsl:with-param select="$mods-type" name="layout" />
            <xsl:with-param select="$mods-type" name="mods-type" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
        <!-- xsl:when cases are handled in modsmetadata.xsl -->
        <xsl:when test="$mods-type = 'report'">
          <xsl:apply-templates select="." mode="present.report" />
        </xsl:when>
        <xsl:when test="$mods-type = 'thesis'">
          <xsl:apply-templates select="." mode="present.thesis" />
        </xsl:when>
        <xsl:when test="$mods-type = 'confpro'">
          <xsl:apply-templates select="." mode="present.confpro" />
        </xsl:when>
        <xsl:when test="$mods-type = 'confpub'">
          <xsl:apply-templates select="." mode="present.confpub" />
        </xsl:when>
        <xsl:when test="$mods-type = 'book'">
          <xsl:apply-templates select="." mode="present.book" />
        </xsl:when>
        <xsl:when test="$mods-type = 'chapter'">
          <xsl:apply-templates select="." mode="present.chapter" />
        </xsl:when>
        <xsl:when test="$mods-type = 'journal'">
          <xsl:apply-templates select="." mode="present.journal" />
        </xsl:when>
        <xsl:when test="$mods-type = 'series'">
          <xsl:apply-templates select="." mode="present.series" />
        </xsl:when>
        <xsl:when test="$mods-type = 'article'">
          <xsl:apply-templates select="." mode="present.article" />
        </xsl:when>
        <xsl:when test="$mods-type = 'av'">
          <xsl:apply-templates select="." mode="present.av" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="present.modsDefaultType">
            <xsl:with-param name="mods-type" select="$mods-type" />
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
      <!--*** Editor Buttons ************************************* -->
      <xsl:if
        test="((./structure/children/child) and not($mods-type='series' or $mods-type='journal' or $mods-type='confpro')) or (./structure/derobjects/derobject)">
        <div id="derivate_box" class="detailbox">
          <h4 id="derivate_switch" class="block_switch">
            <a name="derivate_box"></a>
            <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.derivatebox')" />
          </h4>
          <div id="derivate_content" class="block_content">
            <table class="metaData">
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
                  <xsl:choose>
                    <xsl:when test="count($context/structure/children/child[contains(@xlink:href,$thisObjectType)])=1">
                      <xsl:value-of select="i18n:translate(concat('component.',$thisObjectType,'.metaData.[singular]'))" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="i18n:translate(concat('component.',$thisObjectType,'.metaData.[plural]'))" />
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <xsl:call-template name="printMetaDate">
                  <xsl:with-param select="$context/structure/children/child[contains(@xlink:href, concat('_',$thisObjectType,'_'))]"
                    name="nodes" />
                  <xsl:with-param select="$label" name="label" />
                </xsl:call-template>
              </xsl:for-each>
              <xsl:apply-templates mode="printDerivates" select=".">
                <xsl:with-param select="$staticURL" name="staticURL" />
              </xsl:apply-templates>
            </table>
          </div>
        </div>
      </xsl:if>
      <!--*** Created ************************************* -->
      <div id="system_box" class="detailbox">
        <h4 id="system_switch" class="block_switch">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.systembox')" />
        </h4>
        <div id="system_content" class="block_content">
          <table class="metaData">
            <xsl:call-template name="printMetaDate">
              <xsl:with-param select="./service/servdates/servdate[@type='createdate']" name="nodes" />
              <xsl:with-param select="i18n:translate('metaData.createdAt')" name="label" />
            </xsl:call-template>
            <!--*** Last Modified ************************************* -->
            <xsl:call-template name="printMetaDate">
              <xsl:with-param select="./service/servdates/servdate[@type='modifydate']" name="nodes" />
              <xsl:with-param select="i18n:translate('metaData.lastChanged')" name="label" />
            </xsl:call-template>
            <!--*** MyCoRe-ID ************************************* -->
            <tr>
              <td class="metaname">
                <xsl:value-of select="concat(i18n:translate('metaData.ID'),':')" />
              </td>
              <td class="metavalue">
                <xsl:value-of select="./@ID" />
              </td>
            </tr>
            <tr>
              <td class="metaname">
                <xsl:value-of select="concat(i18n:translate('metaData.versions'),' :')" />
              </td>
              <td class="metavalue">
                <xsl:apply-templates select="." mode="versioninfo" />
              </td>
            </tr>
          </table>
        </div>
      </div>

    </div>

  </xsl:template>
  <xsl:template name="mods.editobject_with_der">
    <xsl:param name="accessedit" />
    <xsl:param name="accessdelete" />
    <xsl:param name="id" />
    <xsl:param name="hasURN" select="'false'" />
    <xsl:param name="displayAddDerivate" select="'true'" />
    <xsl:param name="layout" select="'$'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="editURL">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="$layout" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="acl:checkPermission($id,'writedb') or acl:checkPermission($id,'deletedb')">
          <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />
          <tr>
            <td class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.edit'),':')" />
            </td>
            <td class="metavalue">
              <div class="editorButtons">
                <xsl:if test="acl:checkPermission('default-derivate','writedb') or acl:checkPermission('default-derivate','writedb')">
                  <xsl:choose>
                    <!-- ***************** -->
                    <!-- object has no urn -->
                    <!-- ***************** -->
                    <xsl:when test="not(mcrxsl:hasURNDefined($id))">
                      <a href="{$editURL}">
                        <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('object.editObject')}" />
                      </a>
                      <xsl:if test="$displayAddDerivate='true'">
                        <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                          <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('derivate.addDerivate')}" />
                        </a>
                      </xsl:if>
                      <!-- xsl:if test="mcrxsl:isAllowedObjectForURNAssignment($id)" -->
                      <a
                        href="{$ServletsBaseURL}MCRAddURNToObjectServlet{$HttpSession}?object={$id}&amp;xpath=.mycoreobject/metadata/def.modsContainer[@class='MCRMetaXML' and @heritable='false' and @notinherit='true']/modsContainer/mods:mods/mods:identifier[@type='hdl']">
                        <img src="{$WebApplicationBaseURL}images/workflow_addnbn.gif" title="{i18n:translate('derivate.urn.addURN')}" />
                      </a>
                     <!-- /xsl:if -->
                    </xsl:when>
                    <!-- **************** -->
                    <!-- object has a urn -->
                    <!-- **************** -->
                    <xsl:otherwise>
                      <xsl:if test="$CurrentUser=$MCR.Users.Superuser.UserName">
                        <a href="{$editURL}">
                          <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('object.editObject')}" />
                        </a>
                      </xsl:if>
                      <xsl:if test="$displayAddDerivate=true()">
                        <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                          <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('derivate.addDerivate')}" />
                        </a>
                      </xsl:if>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>
                <xsl:if
                  test="acl:checkPermission($id,'deletedb') and (not(mcrxsl:hasURNDefined($id)) or (mcrxsl:hasURNDefined($id) and $CurrentUser=$MCR.Users.Superuser.UserName))">
                  <a href="{$ServletsBaseURL}object/delete{$HttpSession}?id={$id}">
                    <img src="{$WebApplicationBaseURL}images/workflow_objdelete.gif" title="{i18n:translate('object.delObject')}" />
                  </a>
                </xsl:if>
              </div>
            </td>
          </tr>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <xsl:template name="mods.getObjectEditURL">
    <xsl:param name="id" />
    <xsl:param name="layout" select="'$'" />
    <xsl:choose>
      <xsl:when test="mcrxsl:resourceAvailable('actionmappings.xml')">
      <!-- URL mapping enabled -->
        <xsl:variable name="url">
          <xsl:choose>
            <xsl:when test="$layout = 'all'">
              <xsl:value-of select="actionmapping:getURLforID('update-xml',$id,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="actionmapping:getURLforID('update',$id,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable> 
        <xsl:choose>
          <xsl:when test="string-length($url)=0" />
          <xsl:otherwise>
            <xsl:variable name="urlWithParam">
              <xsl:call-template name="UrlSetParam">
                <xsl:with-param name="url" select="$url"/>
                <xsl:with-param name="par" select="'id'"/>
                <xsl:with-param name="value" select="$id" />
              </xsl:call-template>
            </xsl:variable>
            <xsl:call-template name="UrlAddSession">
              <xsl:with-param name="url" select="$urlWithParam" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
      <!-- URL mapping disabled -->
        <xsl:variable name="layoutSuffix">
          <xsl:if test="$layout != '$'">
            <xsl:value-of select="concat('-',$layout)" />
          </xsl:if>
        </xsl:variable>
        <xsl:variable name="form" select="concat('editor_form_commit-mods',$layoutSuffix,'.xml')" />
        <xsl:value-of select="concat($WebApplicationBaseURL,$form,$HttpSession,'?id=',$id)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template mode="printDerivates" match="/mycoreobject[contains(@ID,'_mods_')]" priority="1">
    <xsl:param name="staticURL" />
    <xsl:param name="layout" />
    <xsl:param name="xmltempl" />
    <xsl:variable name="suffix">
      <xsl:if test="string-length($layout)&gt;0">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:if test="./structure/derobjects">
      <tr>
        <td style="vertical-align:top;" class="metaname">
          <xsl:value-of select="concat(i18n:translate('component.mods.metaData.[derivates]'), ':')" />
        </td>
        <td class="metavalue">
          <xsl:if test="$objectHost != 'local'">
            <a href="{$staticURL}">nur auf original Server</a>
          </xsl:if>
          <xsl:if test="$objectHost = 'local'">
            <xsl:for-each select="./structure/derobjects/derobject">
              <table cellpadding="0" cellspacing="0" border="0" width="100%">
                <tr>
                  <td valign="top" align="left">
                    <xsl:variable select="@xlink:href" name="deriv" />
                    <div class="derivateBox">
                      <xsl:variable select="concat('mcrobject:',$deriv)" name="derivlink" />
                      <xsl:variable select="document($derivlink)" name="derivate" />
                      <xsl:apply-templates select="$derivate/mycorederivate/derivate/internals" />
                      <xsl:apply-templates select="$derivate/mycorederivate/derivate/externals" />
                    </div>
                    <!-- MCR-IView ..start -->
                    <xsl:call-template name="derivateView">
                      <xsl:with-param name="derivateID" select="$deriv" />
                    </xsl:call-template>
                    <!-- MCR - IView ..end -->
                  </td>
                  <xsl:if test="acl:checkPermission(./@xlink:href,'writedb')">
                    <td align="right" valign="top">
                      <a href="{$ServletsBaseURL}derivate/update{$HttpSession}?objectid={../../../@ID}&amp;id={@xlink:href}{$suffix}">
                        <img title="Datei hinzufügen" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" />
                      </a>
                      <a href="{$ServletsBaseURL}derivate/update{$HttpSession}?id={@xlink:href}{$suffix}">
                        <img title="Derivat bearbeiten" src="{$WebApplicationBaseURL}images/workflow_deredit.gif" />
                      </a>
                      <xsl:if test="acl:checkPermission(./@xlink:href,'deletedb')">
                        <a href="{$ServletsBaseURL}derivate/delete{$HttpSession}?id={@xlink:href}">
                          <img title="Derivat löschen" src="{$WebApplicationBaseURL}images/workflow_derdelete.gif" />
                        </a>
                      </xsl:if>
                    </td>
                  </xsl:if>
                </tr>
              </table>
            </xsl:for-each>
          </xsl:if>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="mods.editobject_without_table">
    <xsl:param name="accessedit" />
    <xsl:param name="accessdelete" />
    <xsl:param name="id" />
    <xsl:param name="hasURN" select="'false'" />
    <xsl:param name="displayAddDerivate" select="'true'" />
    <xsl:param name="layout" select="'$'" />
    <xsl:param name="mods-type" select="'report'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="editURL">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="$layout" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="editURL_allMods">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="'all'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="acl:checkPermission($id,'writedb') or acl:checkPermission($id,'deletedb')">
          <div class="document_options">
            <img class="button_options" src="{$WebApplicationBaseURL}templates/master/{$template}/IMAGES/icon_arrow_circled_red_down.png"
              alt="" title="{i18n:translate('component.mods.metaData.options')}" />
            <div class="options">
              <ul>
                <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />

                <xsl:if test="acl:checkPermission($id,'writedb')">

                  <li>
                    <xsl:choose>
                      <xsl:when test="string-length($editURL) &gt; 0">
                        <a href="{$editURL}">
                          <xsl:value-of select="i18n:translate('object.editObject')" />
                        </a>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="i18n:translate('object.locked')" />
                      </xsl:otherwise>
                    </xsl:choose>
                  </li>
                  <xsl:if test="$displayAddDerivate='true' and not(mcrxsl:hasURNDefined($id))">
                    <li>
                      <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                        <xsl:value-of select="i18n:translate('derivate.addDerivate')" />
                      </a>
                    </li>
                  </xsl:if>

                <!-- ToDo: Fix URN/Handle Generator, xpath is not mods valid -->
                <!-- xsl:if test="mcrxsl:isAllowedObjectForURNAssignment($id) and not(mcrxsl:hasURNDefined($id))">
                <a
                  href="{$ServletsBaseURL}MCRAddURNToObjectServlet{$HttpSession}?object={$id}&amp;xpath=.mycoreobject/metadata/def.modsContainer[@class='MCRMetaXML' and @heritable='false' and @notinherit='true']/modsContainer/mods:mods/mods:identifier[@type='hdl']">
                  <img src="{$WebApplicationBaseURL}images/workflow_addnbn.gif" title="{i18n:translate('derivate.urn.addURN')}" />
                </a>
               </xsl:if -->

                </xsl:if>
                <xsl:if
                  test="acl:checkPermission($id,'deletedb') and (not(mcrxsl:hasURNDefined($id)) or (mcrxsl:hasURNDefined($id) and $CurrentUser=$MCR.Users.Superuser.UserName))">
                  <li>
                    <xsl:choose>
                      <xsl:when test="/mycoreobject/structure/children/child">
                        <xsl:value-of select="i18n:translate('object.hasChildren')" />
                      </xsl:when>
                      <xsl:otherwise>
                        <a href="{$ServletsBaseURL}object/delete{$HttpSession}?id={$id}" id="confirm_deletion">
                          <xsl:value-of select="i18n:translate('object.delObject')" />
                        </a>
                      </xsl:otherwise>
                    </xsl:choose>
                  </li>
                </xsl:if>
                <xsl:if test="$CurrentUser=$MCR.Users.Superuser.UserName">
                  <li>
                    <a href="{$editURL_allMods}">
                      <xsl:value-of select="i18n:translate('component.mods.object.editAllModsXML')" />
                    </a>
                  </li>
                </xsl:if>

                <xsl:variable name="child-layout">
                  <xsl:choose>
                    <xsl:when test="$mods-type = 'book'">
                      <xsl:value-of select="'chapter'" />
                    </xsl:when>
                    <xsl:when test="$mods-type = 'confpro'">
                      <xsl:value-of select="'confpub'" />
                    </xsl:when>
                    <xsl:when test="$mods-type = 'journal'">
                      <xsl:value-of select="'article'" />
                    </xsl:when>
                    <xsl:when test="$mods-type = 'series'">
                      <xsl:value-of select="'book|confpro'" />
                    </xsl:when>
                  </xsl:choose>
                </xsl:variable>
                <xsl:if test="string-length($child-layout) &gt; 0 and acl:checkPermission(./@ID,'writedb')">
                  <xsl:choose>
                    <xsl:when test="$mods-type = 'series'">
                      <li>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout=book&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate('component.mods.metaData.types.book')" />
                        </a>
                      </li>
                      <li>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout=confpro&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate('component.mods.metaData.types.confpro')" />
                        </a>
                      </li>
                    </xsl:when>
                    <xsl:otherwise>
                      <li>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout={$child-layout}&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate(concat('component.mods.metaData.types.',$child-layout))" />
                        </a>
                      </li>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>

              </ul>
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
