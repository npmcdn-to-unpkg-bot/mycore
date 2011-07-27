<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:iview2="xalan://org.mycore.frontend.iview2.MCRIView2XSLFunctions" version="1.0"
  exclude-result-prefixes="xlink i18n mcrxml iview2">
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="MCR.Module-iview2.DeveloperMode" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorURI" />
  <xsl:param name="MCR.Module-iview2.PDFCreatorStyle" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:variable name="jqueryUI.version" select="'1.8.12'"/>
  <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" />
  <xsl:template name="iview2.getViewer" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="chapter" select="'true'" />
    <xsl:param name="cutOut" select="'true'" />
    <xsl:param name="overview" select="'true'" />
    <xsl:param name="style" />
    
    <div id="viewerContainer{$groupID}" class="viewerContainer min">
      <xsl:if test="string-length($style) &gt; 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$style"/>
        </xsl:attribute>
      </xsl:if>
      <div id="viewer{$groupID}" class="viewer min" onmousedown="return false;">
        <div class="surface" style="width:100%;height:100%;z-index:30">
        </div>
        <div class="well">
          <div class="preload">
            <img height="100%" width="100%" id="preloadImg{$groupID}" alt="{i18n:translate('component.iview2.preview')}" />
          </div>
        </div>
      </div>
      <xsl:call-template name="iview2.getToolbar">
        <xsl:with-param name="groupID" select="$groupID" />
        <xsl:with-param name="optOut" select="'false'" />
        <xsl:with-param name="forward" select="'true'" />
        <xsl:with-param name="backward" select="'true'" />
      </xsl:call-template>
      <script type="text/javascript">
        <xsl:variable name="baseUris">
          <xsl:choose>
            <xsl:when test="string-length($MCR.Module-iview2.BaseURL)&lt;10">
              <xsl:value-of select="concat($ServletsBaseURL,'MCRTileServlet')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$MCR.Module-iview2.BaseURL" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        var baseUris='["'+'<xsl:value-of select="$baseUris"/>'.split(',').join('","')+'"]';
          var currentNode=(function(){
            var nodes=document.getElementsByTagName('script');
            return nodes[nodes.length-1];
          })();
          var Iview = Iview || {};
          (function initViewer(viewID){
            Iview[viewID] = new iview.IViewInstance(viewID, jQuery(currentNode.parentNode));
            addIviewProperty(viewID, 'useChapter',<xsl:value-of select="$chapter" />);
            addIviewProperty(viewID, 'useCutOut',<xsl:value-of select="$cutOut" />);
            addIviewProperty(viewID, 'useOverview',<xsl:value-of select="$overview" />);
            addIviewProperty(viewID, 'baseUri', baseUris);
            addIviewProperty(viewID, 'webappBaseUri', '"<xsl:value-of select="$WebApplicationBaseURL"/>"');
            addIviewProperty(viewID, 'pdfCreatorURI', '"<xsl:value-of select="$MCR.Module-iview2.PDFCreatorURI"/>"');
            addIviewProperty(viewID, 'pdfCreatorStyle', '"<xsl:value-of select="$MCR.Module-iview2.PDFCreatorStyle"/>"');
          })('<xsl:value-of select="$groupID" />');
      </script>
    </div>

  </xsl:template>
  <xsl:template name="iview2.getToolbar" mode="iview2">
    <xsl:param name="groupID" />
    <xsl:param name="idAdd" />
    <xsl:param name="create" />
    <xsl:param name="optOut" select="'false'" />
    <xsl:param name="zoomIn" select="$optOut" />
    <xsl:param name="zoomOut" select="$optOut" />
    <xsl:param name="normalView" select="$optOut" />
    <xsl:param name="fullView" select="$optOut" />
    <xsl:param name="toWidth" select="$optOut" />
    <xsl:param name="toScreen" select="$optOut" />
    <xsl:param name="backward" select="$optOut" />
    <xsl:param name="forward" select="$optOut" />
    <xsl:param name="openThumbs" select="$optOut" />
    <xsl:param name="chapterOpener" select="$optOut" />
    <xsl:param name="permalink" select="$optOut" />
    
    <!-- online src-->
    <xsl:if test="$MCR.Module-iview2.DeveloperMode='true'">
    	<!--  CSS -->
    	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/lib/fg-menu/fg.menu.css" />
    	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/gfx/default/iview2.toolbar.css" />
    	<link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/gfx/default/iview2.permalink.css" />
        <link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/gfx/default/iview2.createpdf.css" />
    </xsl:if>
	
	<script type="text/javascript">
		<xsl:text>loadCssFile('http://ajax.googleapis.com/ajax/libs/jqueryui/</xsl:text>
		<xsl:value-of select="$jqueryUI.version"/>
		<xsl:text>/themes/base/jquery-ui.css');</xsl:text>
	</script>
     
    <div id="toolbars{$groupID}" class="toolbars" onmousedown="return false;" />      
  </xsl:template>
  <xsl:template name="iview2.init">
    <xsl:param name="groupID" />
    <xsl:param name="tilesize" select="'256'" />
    
    <!-- startUp settings -->
    <xsl:param name="maximized" select="'false'" />
    <xsl:param name="zoomWidth" select="'false'" />
    <xsl:param name="zoomScreen" select="'false'" />
    
    <!-- design settings -->
    <xsl:param name="effects" select="'true'" />
	<!-- chapter settings -->
	<xsl:param name="chapterEmbedded" select="'false'" />
    <xsl:param name="chapDynResize" select="'false'" />
    
    <!-- thumbnail settings -->
    <xsl:param name="DampInViewer" select="'true'" />
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
    <script type="text/javascript">
    <!-- JQuery Framework -->
      google.load("jquery", "1");
      google.load("jqueryui", "<xsl:value-of select="$jqueryUI.version"/>");

      var tilesize=<xsl:value-of select="$tilesize" />;
	  var maximized=<xsl:value-of select="$maximized" />;
      var zoomWidth=<xsl:value-of select="$zoomWidth" />;
      var zoomScreen=<xsl:value-of select="$zoomScreen" />;
      var chapterEmbedded=<xsl:value-of select="$chapterEmbedded" />;
      var chapDynResize=<xsl:value-of select="$chapDynResize" />;
      var DampInViewer=<xsl:value-of select="$DampInViewer" />;
    <!-- Init Funktionen -->
      function addIviewProperty(viewID, propertyName, val) {
        if (typeof (Iview) == "undefined") {
          throw new Error("Iview instance container undefined");
        }
        if (typeof (Iview[viewID]) == "undefined") {
          throw new Error("Iview instance undefined");
        }
        eval('Iview["'+viewID+'"].'+propertyName+'= '+val+';');
      }
    </script>
    
    <xsl:choose>
      <xsl:when test="$MCR.Module-iview2.DeveloperMode='true'">
        <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.js"/>
      </xsl:when>
      <xsl:otherwise>
        <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview2.min.js"/>
      </xsl:otherwise>
      </xsl:choose>
    <script type="text/javascript">
      	var i18n = i18n || new iview.i18n('<xsl:value-of select="$WebApplicationBaseURL"/>', '<xsl:value-of select="$CurrentLang"/>');
    </script>
    <xsl:choose>
      <xsl:when test="$MCR.Module-iview2.DeveloperMode='true'">
		<!-- Main Stylesheet -->
    	<link id="cssSheet{$groupID}" rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}modules/iview2/gfx/default/style.css" />
      </xsl:when>
      <xsl:otherwise>
        <script>
          <xsl:text>loadCssFile('</xsl:text>
          <xsl:value-of select="$WebApplicationBaseURL"/>
          <xsl:text>modules/iview2/gfx/default/iview2.min.css', 'iviewCss');</xsl:text>
        </script>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="iview2.start">
    <xsl:param name="groupID" />
    <xsl:param name="styleFolderUri" select="'gfx/'" />
    <xsl:param name="startFile" />

    <!-- Initfunktionen -->
    <script type="text/javascript">
      var styleFolderUri='<xsl:value-of select="$styleFolderUri" />';
      jQuery(document).ready(function(){
          function startViewer(viewID) {
            if (Iview[viewID].started) return;
            addIviewProperty(viewID, 'startFile', "'<xsl:value-of select="$startFile" />'");
            Iview[viewID].started = true;
            Iview[viewID].preload = jQuery("#viewerContainer" + viewID + " .preload");
            Iview[viewID].gen.loading();
            console.log(Iview[viewID]);
          }
          startViewer('<xsl:value-of select="$groupID"/>');
        }
      );
    </script>
  </xsl:template>

  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />
    <img src="{concat($WebApplicationBaseURL,'servlets/MCRThumbnailServlet/',$derivate,$imagePath)}" style="{$style}" class="{$class}"/>
  </xsl:template>
  
</xsl:stylesheet>