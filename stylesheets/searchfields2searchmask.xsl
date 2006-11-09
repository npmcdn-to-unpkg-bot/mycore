<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet generates a search mask from searchfields.xml configuration file  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:mcr="http://www.mycore.org/" exclude-result-prefixes="mcr">

<xsl:output method="xml" encoding="UTF-8" indent="yes" />

<!-- ==================================================== -->
<!--                  XSL Parameters                      -->
<!-- ==================================================== -->

<!-- Build a webpage file or the included editor file? -->
<xsl:param name="mode" select="'editor'" />

<!-- Filename of the editor definition file -->
<xsl:param name="filename.editor" />

<!-- Filename of the webpage that contains the search mask -->
<xsl:param name="filename.webpage" />

<!-- Title of webpage in german -->
<xsl:param name="title.de" />

<!-- Title of webpage in english -->
<xsl:param name="title.en" />

<!-- i18n key of search mask headline -->
<xsl:param name="headline.i18n" />

<!-- ID of the search index(es) to include, multiple indexes separated by blanks -->
<xsl:param name="search.indexes" />

<!-- Name(s) of the search fields to include, separated by blanks. -->
<!-- If this property is not set, ALL search fields will be included in search mask -->
<xsl:param name="search.fields" />

<!-- Name(s) of the search fields to skip, separated by blanks. -->
<!-- If this property is not set, NO search fields will be skipped in search mask -->
<xsl:param name="skip.fields" />

<!-- Optional restriction (hidden condition) for search -->
<!-- Syntax: "field operator value", separated by blanks -->
<xsl:param name="restriction" />

<!-- If 'true', include a panel to select sort criteria of results -->
<xsl:param name="include.sortByPanel" select="'true'" />

<!-- If true, include a panel to select hosts to query -->
<xsl:param name="include.hostsSelectionPanel" select="'true'" />

<!-- ==================================================== -->
<!--                 Global variables                     -->
<!-- ==================================================== -->

<xsl:variable name="fieldtypes" select="document('fieldtypes.xml',.)/mcr:fieldtypes" />
<xsl:variable name="fieldlen" select="string-length(normalize-space($search.fields))" />
<xsl:variable name="skiplen" select="string-length(normalize-space($skip.fields))" />

<!-- ==================================================== -->
<!--                   Transformation                     -->
<!-- ==================================================== -->

<xsl:template match="/">
  <xsl:if test="$mode = 'editor'">
    <xsl:apply-templates select="mcr:searchfields" />
  </xsl:if>
  <xsl:if test="$mode = 'webpage'">
    <xsl:call-template name="webpage" />
  </xsl:if>
</xsl:template>

<!-- ==================================================== -->
<!--                   Build webpage                      -->
<!-- ==================================================== -->

<xsl:template name="webpage">
  <MyCoReWebPage>
    <section title="{$title.de}" xml:lang="de">
      <editor id="searchmask-de">
        <include uri="webapp:editor/{$filename.editor}"/>
      </editor>
    </section>
    <section title="{$title.en}" xml:lang="en">
      <editor id="searchmask-en">
        <include uri="webapp:editor/{$filename.editor}"/>
      </editor>
    </section>
  </MyCoReWebPage>
</xsl:template>

<!-- ==================================================== -->
<!--                    Build editor                      -->
<!-- ==================================================== -->

<xsl:template match="mcr:searchfields">
  <editor id="searchmask">

  <xsl:value-of select="$newline" />
  <xsl:value-of select="$newline" />

    <source url="request:servlets/MCRSearchServlet?mode=load&amp;id=[ID]" token="[ID]" />
    <target type="servlet" name="MCRSearchServlet" method="post" format="xml" />

    <xsl:value-of select="$newline" />
    <xsl:value-of select="$newline" />

    <components root="root" var="/query">
      <include uri="webapp:editor/imports-common.xml"/>
      <headline anchor="LEFT">
        <text i18n="{$headline.i18n}"/>
      </headline>

      <xsl:value-of select="$newline" />
      <xsl:value-of select="$newline" />
      
      <panel id="root" lines="off">
      
        <xsl:value-of select="$newline" />
        <xsl:value-of select="$newline" />

        <hidden var="@mask" default="{$filename.webpage}" />
        <hidden var="conditions/@format" default="xml" />
        <hidden var="conditions/boolean/@operator" default="and" />
        <xsl:value-of select="$newline" />
 
        <xsl:apply-templates select="mcr:index[contains(concat(' ',$search.indexes,' '),concat(' ',@id,' '))]/mcr:field" />
        
        <xsl:if test="string-length(normalize-space($restriction)) &gt; 0">
          <xsl:call-template name="restriction" />
        </xsl:if>
        
        <xsl:if test="$include.hostsSelectionPanel = 'true'">
          <xsl:call-template name="hosts" />
        </xsl:if>
        
        <xsl:if test="$include.sortByPanel = 'true'">
          <xsl:call-template name="sortBy" />
        </xsl:if>
        
        <xsl:call-template name="maxResultsNumPerPage" />

        <cell row="99" col="1" colspan="2" anchor="EAST">
          <submitButton i18n="editor.search.search" width="150px" />
        </cell>
        
        <xsl:value-of select="$newline" />
        <xsl:value-of select="$newline" />
        
      </panel>
 
      <xsl:call-template name="includes" />      
     
    </components>
  </editor>
</xsl:template>

<!-- ==================================================== -->
<!--     Build hidden search condition as restriction     -->
<!-- ==================================================== -->

<xsl:template name="restriction">
  <xsl:value-of select="$newline" />
  <xsl:value-of select="$newline" />

  <xsl:comment> Search only for <xsl:value-of select="$restriction" />
    <xsl:text> </xsl:text>
  </xsl:comment>
  <xsl:value-of select="$newline" />
  
  <hidden var="conditions/boolean/condition94/@field"    
    default="{normalize-space(substring-before(normalize-space($restriction),' '))}" />
  <hidden var="conditions/boolean/condition94/@operator" 
    default="{normalize-space(substring-before(normalize-space(substring-after(normalize-space($restriction),' ')),' '))}" />
  <hidden var="conditions/boolean/condition94/@value"
    default="{normalize-space(substring-after(normalize-space(substring-after(normalize-space($restriction),' ')),' '))}" />

  <xsl:value-of select="$newline" />
  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--   Input elements included depending on field type    -->
<!-- ==================================================== -->

<xsl:template name="includes">
  <xsl:value-of select="$newline" />
  <xsl:value-of select="$newline" />

  <xsl:comment> Input elements included depending on field type </xsl:comment>
  <xsl:value-of select="$newline" />
  
  <textfield width="40" id="input.text" />
  <textfield width="30" id="input.name" />
  <textfield width="20" id="input.identifier" />
  <textfield width="10" id="input.date" />
  <textfield width="8"  id="input.time" />
  <textfield width="18" id="input.timestamp" />
  <list type="checkbox" rows="1" default="" id="input.boolean">
    <item i18n="editor.search.choose" value="" />
    <item i18n="editor.search.true"   value="true" />
    <item i18n="editor.search.false"  value="false" />
  </list>
  <textfield width="10" id="input.decimal" />
  <textfield width="10" id="input.integer" />

  <xsl:value-of select="$newline" />
  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--             Selector for hosts to query              -->
<!-- ==================================================== -->

<xsl:template name="hosts">
  <xsl:value-of select="$newline" />

  <xsl:comment> Select hosts to query </xsl:comment>
  <xsl:value-of select="$newline" />

  <cell row="95" col="1" anchor="SOUTHEAST" height="50px">
    <text i18n="editor.search.searchon"/>
  </cell>
  <cell row="95" col="2" anchor="SOUTHWEST" var="hosts/@target">
    <list type="radio" default="local">
      <item value="local" i18n="editor.search.searchthis"/>
      <item value="all" i18n="editor.search.searchall"/>
      <item value="selected" i18n="editor.search.searchboth"/>
    </list>
  </cell>
      
  <cell row="96" col="2" anchor="NORTHWEST" var="hosts/host">
    <list type="checkbox" cols="2">
      <include uri="request:hosts.xml" />
    </list>
  </cell>

  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--        Selector for sort criteria of results         -->
<!-- ==================================================== -->

<xsl:template name="sortBy">
  <xsl:value-of select="$newline" />

  <xsl:comment> Select sort order of results </xsl:comment>
  <xsl:value-of select="$newline" />
  
  <cell row="97" col="1" anchor="NORTHEAST">
    <text i18n="editor.search.sortby" />
  </cell>
  <cell row="97" col="2" anchor="NORTHWEST" var="sortBy/field">
    <repeater min="1" max="3">
      <panel lines="off">
        <cell row="1" col="1" anchor="WEST" var="@name">
          <list type="dropdown">
            <item value="" i18n="editor.search.choose" />
            <xsl:for-each select="mcr:index[contains(concat(' ',$search.indexes,' '),concat(' ',@id,' '))]/mcr:field[@sortable='true']">
              <item value="{@name}" i18n="{@i18n}" />
            </xsl:for-each>
          </list>
        </cell>
        <cell row="1" col="2" anchor="WEST" var="@order">
          <list type="dropdown" default="ascending">
            <item value="ascending" i18n="editor.search.ascending" />
            <item value="descending" i18n="editor.search.descending" />
          </list>
        </cell>
      </panel>
    </repeater>
  </cell>

  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--      Selector for maxResults and numPerPage          -->
<!-- ==================================================== -->

<xsl:template name="maxResultsNumPerPage">
  <xsl:value-of select="$newline" />

  <xsl:comment> Select maximum number of results and num per page </xsl:comment>
  <xsl:value-of select="$newline" />

  <cell row="98" col="1" colspan="2" anchor="SOUTHEAST" height="50px">
    <panel lines="off">
      <cell row="1" col="1" anchor="WEST">
        <text  i18n="editor.search.max" />
      </cell>
      <cell row="1" col="2" anchor="WEST" var="@maxResults">
        <list type="dropdown" default="100">
          <item value="20"  label="20"  />
          <item value="100" label="100" />
          <item value="500" label="500" />            
          <item value="0"   i18n="editor.search.all" />
        </list>
      </cell>
      <cell row="1" col="3" anchor="WEST">
        <text i18n="editor.search.label" />
      </cell>
      <cell row="1" col="4" anchor="WEST" var="@numPerPage">
        <list type="dropdown" default="10">
          <item value="10" label="10" />
          <item value="20" label="20" />
          <item value="50" label="50" />
          <item value="0"  i18n="editor.search.all" />
        </list>
      </cell>
      <cell row="1" col="5" anchor="WEST">
        <text i18n="editor.search.perpage" />
      </cell>
    </panel>
  </cell>

  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--      Input element for a single search field         -->
<!-- ==================================================== -->

<xsl:template match="mcr:field">
  <xsl:if test="@source != 'searcherHitMetadata'">
    <xsl:if test="($fieldlen = 0) or (($fieldlen &gt; 0) and contains(concat(' ',$search.fields,' '),concat(' ',@name,' ')))">
      <xsl:choose>
        <xsl:when test="$skiplen = 0">
          <xsl:call-template name="build.search" />
        </xsl:when>
        <xsl:when test="contains(concat(' ',$skip.fields,' '),concat(' ',@name,' '))" />
        <xsl:otherwise>
          <xsl:call-template name="build.search" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:if>
</xsl:template>  

<xsl:template name="build.search">
  <xsl:value-of select="$newline" />
  <xsl:comment> Search in field '<xsl:value-of select="@name" />
    <xsl:text>' with operator '</xsl:text>
    <xsl:value-of select="$fieldtypes/mcr:type[@name=current()/@type]/@default" />
    <xsl:text>' </xsl:text>
  </xsl:comment>
  <xsl:value-of select="$newline" />

  <hidden var="conditions/boolean/condition{position()}/@field" default="{@name}" />
  <xsl:value-of select="$newline" />
  <hidden var="conditions/boolean/condition{position()}/@operator" default="{$fieldtypes/mcr:type[@name=current()/@type]/@default}" />
  <xsl:value-of select="$newline" />
   <cell row="{position()}" col="1" anchor="EAST">
    <text i18n="{@i18n}" />
  </cell>
  <xsl:value-of select="$newline" />
  <cell row="{position()}" col="2" anchor="WEST" 
    var="conditions/boolean/condition{position()}/@value"
    ref="input.{@type}" />
  <xsl:value-of select="$newline" />
</xsl:template>

<!-- ==================================================== -->
<!--               Insert new line in output              -->
<!-- ==================================================== -->

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

</xsl:stylesheet>
