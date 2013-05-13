<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
    &html-output;
    <xsl:include href="MyCoReLayout.xsl" />

    <!-- 
        see mcr_acl_editor_common.xsl for definition of following variables
        
        redirectURL
        servletName
        editorURL
        aclEditorURL
        dataRequest
        permEditor
        ruleEditor
        
        add
        edit
        delete
    -->

    <xsl:include href="mcr_acl_editor_common.xsl" />
    <xsl:include href="mcr_access_set.xsl" />
    <xsl:include href="mcr_access_rule_set.xsl" />

    <xsl:variable name="PageTitle">
        <xsl:choose>
            <xsl:when test="//editor = $permEditor">
                <xsl:value-of select="i18n:translate('component.acl-editor.permEditor')" />
            </xsl:when>
            <xsl:when test="//editor = $ruleEditor">
                <xsl:value-of select="i18n:translate('component.acl-editor.ruleEditor')" />
            </xsl:when>
        </xsl:choose>
    </xsl:variable>

    <!--    <xsl:variable name="javaScript" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/JS/aclEditor.js')" />-->
    <!--    <xsl:variable name="css" select="concat($WebApplicationBaseURL,'modules/acl-editor/web/CSS/acl_editor.css')" />-->

    <xsl:template match="mcr_acl_editor">
        <xsl:variable name="filter">
            <xsl:call-template name="buildFilter">
                <xsl:with-param name="objIdFilter" select="mcr_access_filter/objid" />
                <xsl:with-param name="acPoolFilter" select="mcr_access_filter/acpool" />
            </xsl:call-template>
        </xsl:variable>

        <div id="ACL-Editor">

            <xsl:choose>
                <xsl:when test="editor = $permEditor">
                    <a href="{concat($aclEditorURL, '&amp;editor=ruleEditor', $filter)}">
                        <xsl:value-of select="i18n:translate('component.acl-editor.ruleEditor')" />
                    </a>

                     <xsl:variable name="permEditor" select="document(concat('acl-module:getPermEditor:', $filter))" />
                     <xsl:apply-templates select="$permEditor/*" />

              <!--   <xsl:variable name="permEditor" select="concat($dataRequest, '&amp;action=getPermEditor', $filter)" /> 
                    <xsl:copy-of select="document($permEditor)" />-->
                </xsl:when>
                <xsl:when test="editor = $ruleEditor">
                    <a href="{concat($aclEditorURL, '&amp;editor=permEditor', $filter)}">
                        <xsl:value-of select="i18n:translate('component.acl-editor.permEditor')" />
                    </a>

                    <xsl:variable name="ruleEditor" select="document('acl-module:getRuleEditor')" />
                    <xsl:apply-templates select="$ruleEditor/*" />
                    <!-- <xsl:variable name="ruleEditor" select="concat($dataRequest, '&amp;action=getRuleEditor')" />
                    <xsl:copy-of select="document($ruleEditor)" />-->

                </xsl:when>
                <xsl:when test="editor = $embPermEditor">
                    <xsl:variable name="embPermEditor" select="document(concat('acl-module:getPermEditor:emb=true&amp;cmd=', cmd, $filter,$redirectURL))" />
                    <xsl:apply-templates select="$embPermEditor/*" />
 
                    <!-- <xsl:variable name="embPermEditor" select="concat($dataRequest, '&amp;action=getPermEditor&amp;emb=true&amp;cmd=', cmd, $filter)" />
                    <xsl:copy-of select="document(concat($embPermEditor, '&amp;redir=', $redirectURL))" />-->

                </xsl:when>
            </xsl:choose>

        </div>
    </xsl:template>

    <xsl:template name="getEmbPermEditor">
        <xsl:param name="objId" />
        <xsl:param name="acPool" />

        <xsl:if test="($objId != '') and ($acPool != '')">
            <xsl:variable name="filter">
                <xsl:call-template name="buildFilter">
                    <xsl:with-param name="objIdFilter" select="$objId" />
                    <xsl:with-param name="acPoolFilter" select="$acPool" />
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="embPermEditor" select="concat($aclEditorURL, '&amp;editor=embPermEditor', $filter)" />
            <xsl:copy-of select="document($embPermEditor)" />
        </xsl:if>

    </xsl:template>

</xsl:stylesheet>
