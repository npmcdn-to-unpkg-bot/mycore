<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.11 $ $Date: 2007-05-10 11:16:50 $ -->
<!-- ============================================== -->

<!-- +
  | This stylesheet controls the Web-Layout of the MCRClassificationBrowser Servlet.     |
  | This Template is embedded as a Part in the XML-Site, configurated in the Classification
  | section of the mycore.properties.
  | The complete XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
  |
  | Authors: A.Schaar
  | Last changes: 2005-30-10
  + -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="xlink">

  <xsl:variable name="Navigation.title" select="i18n:translate('titles.pageTitle.classEdit')" />
  <xsl:variable name="MainTitle" select="i18n:translate('titles.mainTitle')" />
  <xsl:variable name="PageTitle" select="i18n:translate('titles.pageTitle.classEdit')" />

  <!-- The main template -->
  <xsl:template match="classificationBrowser">

    <xsl:variable name="startPath" select="startPath" />
    <xsl:variable name="TrueFalse" select="showComments" />

    <div id="classificationBrowser">

      <xsl:variable name="type" select="startPath" />
      <xsl:variable name="search" select="searchField" />

      <!-- single classification listed -->
      <xsl:apply-templates select="navigationtree" />

      <!-- display list of classifications -->
      <xsl:apply-templates select="classificationlist" />
    </div>

  </xsl:template>

  <xsl:template match="navigationtree">
    <xsl:variable name="predecessor" select="@predecessor" />
    <xsl:variable name="classifID" select="@classifID" />
    <xsl:variable name="label" select="../label" />
    <xsl:variable name="actcateg" select="@categID" />
    <xsl:variable name="view" select="@view" />
    <xsl:variable name="doctype" select="@doctype" />
    <xsl:variable name="restriction" select="@restriction" />
    <xsl:variable name="search_attribute" select="@searchField" />
    <xsl:variable name="userEdited" select="../userEdited" />
    <xsl:variable name="session" select="../session" />
    <xsl:variable name="currentSession" select="../currentSession" />

    <xsl:variable name="hrefstart" select="concat($WebApplicationBaseURL, 'browse?mode=edit')" />
    <xsl:variable name="search" select="@searchField" />
    <xsl:variable name="type" select="startPath" />
    <xsl:variable name="canCreate" select="../userCanCreate" />
    <xsl:variable name="canEdit" select="../userCanEdit" />
    <xsl:variable name="canDelete" select="../userCanDelete" />
    <xsl:variable name="uri" select="uri" />
    <xsl:variable name="path" select="concat($WebApplicationBaseURL, 'browse/', $uri,'?mode=edit')" />
    <xsl:variable name="edited" select="edited" />

    <xsl:variable name="classnew" select="concat($WebApplicationBaseURL, 'images/classnew.gif')" />
    <xsl:variable name="classedit" select="concat($WebApplicationBaseURL, 'images/classedit.gif')" />
    <xsl:variable name="classdelete" select="concat($WebApplicationBaseURL, 'images/classdelete.gif')" />
    <xsl:variable name="classup" select="concat($WebApplicationBaseURL, 'images/classup.gif')" />
    <xsl:variable name="classdown" select="concat($WebApplicationBaseURL, 'images/classdown.gif')" />
    <xsl:variable name="classleft" select="concat($WebApplicationBaseURL, 'images/classleft.gif')" />
    <xsl:variable name="classright" select="concat($WebApplicationBaseURL, 'images/classright.gif')" />
    <xsl:variable name="classexport" select="concat($WebApplicationBaseURL, 'images/classexport.gif')" />
    <xsl:variable name="imgEmpty" select="concat($WebApplicationBaseURL, 'images/emtyDot1Pix.gif')" />

    <table cellspacing="0" cellpadding="0" style="width:100%; margin: 3% 0px 3% 2%;" class="bg_background">
      <tr>
        <td style="text-align:left;padding-right:20px;">
          <b>
            <xsl:value-of select="$label" />
          </b>
        </td>
        <td style="text-align:right;padding-right:5px;">
          <a href='{$hrefstart}'>
            <xsl:value-of select="i18n:translate('Browse.showAllClass')" />
          </a>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <xsl:value-of select="concat('[',$classifID,']')" />
          <xsl:if test="$userEdited != 'false'">
            <br />
            <b>
              <xsl:value-of select="i18n:translate('Browse.editError')" />
            </b>
            &#160;
            <xsl:value-of select="$userEdited" />
          </xsl:if>
          <table cellspacing="1" cellpadding="2" style="margin: 3% 10px 3% 2%;">

            <xsl:for-each select="row">

              <xsl:variable name="href1" select="concat($WebApplicationBaseURL, 'browse', col[2]/@searchbase,$HttpSession,'?mode=edit&amp;clid=',$classifID)" />
              <xsl:variable name="actnode" select="position()" />

              <xsl:variable name="href2"
                select="concat($ServletsBaseURL, 'MCRSearchServlet',$HttpSession,'?query=(',$search,'+=+', col[2]/@lineID, ')+and+',$doctype,'&amp;numPerPage=10','&amp;mask=browse/',$type)" />
              <xsl:variable name="img1" select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder1, '.gif')" />
              <xsl:variable name="img2" select="concat($WebApplicationBaseURL, 'images/', col[1]/@folder2, '.gif')" />
              <xsl:variable name="img3" select="concat($WebApplicationBaseURL, 'images/folder_blank.gif')" />
              <xsl:variable name="childpos" select="col[1]/@childpos" />

              <xsl:variable name="trStyle">
                <xsl:choose>
                  <xsl:when test="$actcateg = col[2]/@lineID">actrow</xsl:when>
                  <xsl:otherwise>classeditor</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
              <tr valign="top">
                <td class="{$trStyle}" nowrap="yes">
                  <xsl:call-template name="lineLevelLoop">
                    <xsl:with-param name="anz" select="col[1]/@lineLevel" />
                    <xsl:with-param name="img" select="$img3" />
                  </xsl:call-template>
                  <xsl:choose>
                    <xsl:when test="col[1]/@plusminusbase">
                      <a href='{$href1}'>
                        <img border="0" src='{$img1}' />
                      </a>
                    </xsl:when>
                    <xsl:otherwise>
                      <img border="0" src='{$img1}' />
                    </xsl:otherwise>
                  </xsl:choose>
                  <xsl:variable name="h2" select="string-length(col[2]/@numDocs)" />
                  <xsl:variable name="h3" select="4 - $h2" />
                  <xsl:variable name="h4" select="col[2]/@numDocs" />
                  <xsl:variable name="h6">
                    <xsl:if test="$h3 > 0">
                      <xsl:value-of select="substring('____', 1, $h3)" />
                    </xsl:if>
                    <xsl:value-of select="$h4" />
                  </xsl:variable>
                  &#160;
                  <xsl:value-of select="i18n:translate('Browse.docs',$h6)" />
                </td>
                <td class="{$trStyle}">
                  <xsl:choose>
                    <xsl:when test="col[2]/@numDocs > 0 and col[2]/@searchbase != 'default' ">
                      <a href='{$href2}'>
                        <xsl:value-of select="col[2]/text()" />
                      </a>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="col[2]/text()" />
                    </xsl:otherwise>
                  </xsl:choose>
                </td>
                <td class="{$trStyle}">
                  <xsl:choose>
                    <xsl:when test="col[2]/comment != ''">
                      <i>
                        (
                        <xsl:apply-templates select="col[2]/comment" />
                        )
                      </i>
                    </xsl:when>
                    <xsl:otherwise>&#160;&#160;</xsl:otherwise>
                  </xsl:choose>
                </td>
                <td class="{$trStyle}" width="25">&#160;&#160;&#160;</td>
                <td class="{$trStyle}">
                  <xsl:choose>
                    <xsl:when test="col[1]/@folder1 = 'folder_minus' ">
                      <!-- leer  weil hier nur die Rückreferenz aufs parent kommt
                        geht anders bestimmt schöner -->
                      &#160;
                    </xsl:when>
                    <xsl:otherwise>
                      <!-- Wurzel-, Kindknoten mit und ohne Dokumenten -->

                      <xsl:if test="($canEdit = 'true') and (($userEdited='false') or (($session='') or ($session=$currentSession))) ">
                        <table cellpadding="0" cellspacing="0" height="16">
                          <tr valign="middle" height="16">
                            <td width="23">
                              <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                <input type="hidden" name="todo" value='create-category' />
                                <input type="hidden" name="todo2" value='modify-classification' />
                                <input type="hidden" name="path" value='{$path}' />
                                <input type="hidden" name="clid" value='{$classifID}' />
                                <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                <input type="image" src='{$classnew}' title="{i18n:translate('Browse.newUnderCatInsert')}" />
                              </form>
                            </td>
                            <td width="23">
                              <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                <input type="hidden" name="todo" value='modify-category' />
                                <input type="hidden" name="todo2" value='modify-classification' />
                                <input type="hidden" name="path" value='{$path}' />
                                <input type="hidden" name="clid" value='{$classifID}' />
                                <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                <input type="image" src='{$classedit}' title="{i18n:translate('Browse.editCat')}" />
                              </form>
                            </td>
                            <td width="23">
                              <xsl:choose>
                                <xsl:when test="(col[2]/@numDocs = 0) and ($canEdit = 'true')">
                                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                    <input type="hidden" name="todo" value='delete-category' />
                                    <input type="hidden" name="todo2" value='modify-classification' />
                                    <input type="hidden" name="path" value='{$path}' />
                                    <input type="hidden" name="clid" value='{$classifID}' />
                                    <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                    <input type="image" src='{$classdelete}' title="{i18n:translate('Browse.deleteCat')}" />
                                  </form>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img src="{$imgEmpty}" border="0" width="23" />
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td width="16">
                              <xsl:choose>
                                <xsl:when test="$childpos = 'last' or $childpos = 'middle' ">
                                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                    <input type="hidden" name="todo" value='up-category' />
                                    <input type="hidden" name="todo2" value='modify-classification' />
                                    <input type="hidden" name="path" value='{$path}' />
                                    <input type="hidden" name="clid" value='{$classifID}' />
                                    <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                    <input type="image" src='{$classup}' title="{i18n:translate('Browse.moveUp')}" />
                                  </form>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img src="{$imgEmpty}" border="0" width="16" />
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td width="16">
                              <xsl:choose>
                                <xsl:when test="$childpos = 'first' or $childpos = 'middle' ">
                                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                    <input type="hidden" name="todo" value='down-category' />
                                    <input type="hidden" name="todo2" value='modify-classification' />
                                    <input type="hidden" name="path" value='{$path}' />
                                    <input type="hidden" name="clid" value='{$classifID}' />
                                    <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                    <input type="image" src='{$classdown}' title="{i18n:translate('Browse.moveDown')}" />
                                  </form>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img src="{$imgEmpty}" border="0" width="16" />
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td width="16">
                              <xsl:choose>
                                <xsl:when test="col[1]/@lineLevel > 0 ">
                                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                    <input type="hidden" name="todo" value='left-category' />
                                    <input type="hidden" name="todo2" value='modify-classification' />
                                    <input type="hidden" name="path" value='{$path}' />
                                    <input type="hidden" name="clid" value='{$classifID}' />
                                    <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                    <input type="image" src='{$classleft}' title="{i18n:translate('Browse.moveLeft')}" />
                                  </form>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img src="{$imgEmpty}" border="0" width="16" />
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                            <td width="16">
                              <xsl:choose>
                                <xsl:when test="$childpos = 'last' or $childpos = 'middle' ">
                                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                                    <input type="hidden" name="todo" value='right-category' />
                                    <input type="hidden" name="todo2" value='modify-classification' />
                                    <input type="hidden" name="path" value='{$href1}' />
                                    <input type="hidden" name="clid" value='{$classifID}' />
                                    <input type="hidden" name="categid" value='{col[2]/@lineID}' />
                                    <input type="image" src='{$classright}' title="{i18n:translate('Browse.moveRight')}" />
                                  </form>
                                </xsl:when>
                                <xsl:otherwise>
                                  <img src="{$imgEmpty}" border="0" width="16" />
                                </xsl:otherwise>
                              </xsl:choose>
                            </td>
                          </tr>
                        </table>
                      </xsl:if>
                    </xsl:otherwise>
                  </xsl:choose>
                </td>
              </tr>
            </xsl:for-each>

            <tr>
              <td colspan="6">
                <xsl:if test="($canEdit = 'true')">
                  <hr />
                  <center>
                    <table>
                      <tr>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="todo" value='create-category' />
                            <input type="hidden" name="todo2" value='modify-classification' />
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='{$classifID}' />
                            <input type="hidden" name="categid" value='empty' />
                            <input type="submit" class="button" name="newcateg" value="{i18n:translate('Browse.newCat')}" />
                          </form>
                        </td>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='' />
                            <input type="hidden" name="todo" value='save-all' />
                            <input type="submit" class="button" name="saveAll" value="{i18n:translate('Browse.saveClass')}" />
                          </form>
                        </td>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='' />
                            <input type="hidden" name="todo" value='purge-all' />
                            <input type="submit" class="button" name="purgeAll" value="{i18n:translate('Browse.discardClass')}" />
                          </form>
                        </td>
                      </tr>
                    </table>
                  </center>
                  <hr />
                </xsl:if>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="classificationlist">
    <xsl:variable name="path" select="concat($WebApplicationBaseURL, 'browse/',$HttpSession, '?mode=edit' )" />
    <xsl:variable name="canCreate" select="../userCanCreate" />

    <xsl:variable name="classnew" select="concat($WebApplicationBaseURL, 'images/classnew.gif')" />
    <xsl:variable name="classedit" select="concat($WebApplicationBaseURL, 'images/classedit.gif')" />
    <xsl:variable name="classdelete" select="concat($WebApplicationBaseURL, 'images/classdelete.gif')" />
    <xsl:variable name="classacl" select="concat($WebApplicationBaseURL, 'images/classacl.gif')" />
    <xsl:variable name="classup" select="concat($WebApplicationBaseURL, 'images/classup.gif')" />
    <xsl:variable name="classdown" select="concat($WebApplicationBaseURL, 'images/classdown.gif')" />
    <xsl:variable name="classleft" select="concat($WebApplicationBaseURL, 'images/classleft.gif')" />
    <xsl:variable name="classright" select="concat($WebApplicationBaseURL, 'images/classright.gif')" />
    <xsl:variable name="classexport" select="concat($WebApplicationBaseURL, 'images/classexport.gif')" />
    <xsl:variable name="imgEmpty" select="concat($WebApplicationBaseURL, 'images/emtyDot1Pix.gif')" />
    <table cellspacing="0" cellpadding="0" border="0">
      <xsl:for-each select="classification">

        <xsl:variable name="browserClass" select="@browserClass" />
        <xsl:variable name="classifID" select="@ID" />
        <xsl:variable name="counter" select="@counter" />
        <xsl:variable name="categpath" select="concat($WebApplicationBaseURL, 'browse/', $browserClass,$HttpSession, '?mode=edit&amp;clid=',$classifID )" />
        <xsl:variable name="edited" select="@edited" />
        <xsl:variable name="userEdited" select="@userEdited" />
        <xsl:variable name="canEdit" select="@userCanEdit" />
        <xsl:variable name="canDelete" select="@userCanDelete" />
        <xsl:variable name="h2" select="string-length(@counter)" />
        <xsl:variable name="h3" select="4 - $h2" />
        <xsl:variable name="h4" select="@counter" />
        <xsl:variable name="h6">
          <xsl:if test="$h3 > 0">
            <xsl:value-of select="substring('____', 1, $h3)" />
          </xsl:if>
          <xsl:value-of select="$h4" />
        </xsl:variable>
        <tr valign="top">
          <td nowrap="yes" class="classeditor">
            <xsl:value-of select="i18n:translate('Browse.docs',$h6)" />
          </td>
          <td class="classeditor">
            <xsl:choose>
              <xsl:when test="$browserClass != ''">
                <a href='{$categpath}'>
                  <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']')" />
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']&#160;(Browserpfad fehlt)')" />
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="$edited='true'">
              <span class="classEdited">
                <xsl:value-of select="concat(' ',i18n:translate('Browse.edited'))" />
              </span>
            </xsl:if>
            <br />
            <xsl:if test="label/@description != ''">
              <xsl:value-of select="label/@description" />
            </xsl:if>
          </td>
          <td nowrap="yes" class="classeditor">
            &#160;
            <xsl:if test="($canEdit = 'true') and ($userEdited = 'false')">
              <table cellpadding="0" cellspacing="0" height="16">
                <tr valign="middle" height="16">
                  <td width="25">&#160;</td>
                  <td width="25" valign="top">
                    <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                      <input type="hidden" name="todo" value='modify-classification' />
                      <input type="hidden" name="path" value='{$categpath}' />
                      <input type="hidden" name="clid" value='{$classifID}' />
                      <input type="image" src='{$classedit}' title="{i18n:translate('Browse.classDescEdit')}" />
                    </form>
                  </td>
                  <xsl:if test="$edited='false'">
                    <td width="25" valign="top">
                      <a target="new" alt="$classifID" onclick="fensterCodice('{$WebApplicationBaseURL}receive/{$classifID}{$HttpSession}');return false;"
                        href="{$WebApplicationBaseURL}receive/{$classifID}{$HttpSession}">
                        <input onclick="fensterCodice('{$WebApplicationBaseURL}receive/{$classifID}{$HttpSession});return false;" type="image"
                          src='{$classexport}' title="{i18n:translate('Browse.classExport')}" />
                      </a>
                    </td>
                  </xsl:if>
                  <td width="25" valign="top">
                    <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                      <input type="hidden" name="todo" value='acl-classification' />
                      <input type="hidden" name="clid" value='{$classifID}' />
                      <input type="image" src='{$classacl}' title="{i18n:translate('Browse.classACL')}" />
                    </form>
                  </td>
                  <td width="25" valign="top">
                    <xsl:if test="($canDelete = 'true')">
                      <xsl:if test="$counter = 0">
                        <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                          <input type="hidden" name="todo" value='delete-classification' />
                          <input type="hidden" name="path" value='{$categpath}' />
                          <input type="hidden" name="clid" value='{$classifID}' />
                          <input type="image" src='{$classdelete}' title="{i18n:translate('Browse.classDelete')}" />
                        </form>
                      </xsl:if>
                      <xsl:if test="$counter > 0">
                        <img src="{$imgEmpty}" border="0" width="21" />
                      </xsl:if>
                    </xsl:if>
                  </td>
                </tr>
              </table>
            </xsl:if>
          </td>
        </tr>
      </xsl:for-each>
      <xsl:if test="($canCreate = 'true')">
        <tr>
          <td colspan="4" align="center">
            <hr />
            <br />
            <table>
              <tr>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='create-classification' />
                    <input type="submit" class="button" name="newClass" value="{i18n:translate('Browse.newClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='import-classification' />
                    <input type="submit" class="button" name="importClass" value="{i18n:translate('Browse.importClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='save-all' />
                    <input type="submit" class="button" name="saveAll" value="{i18n:translate('Browse.saveClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='purge-all' />
                    <input type="submit" class="button" name="purgeAll" value="{i18n:translate('Browse.discardClass')}" />
                  </form>
                </td>
              </tr>
            </table>
            <hr />
          </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:template>



  <!-- - - - - - - - - Identity Transformation  - - - - - - - - - -->

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="comment">
    <xsl:apply-templates select='@*|node()' />
  </xsl:template>

  <xsl:template name="lineLevelLoop">
    <xsl:param name="anz" />
    <xsl:param name="img" />

    <xsl:if test="$anz > 0">
      <img border="0" width="10" src='{$img}' />
      <xsl:call-template name="lineLevelLoop">
        <xsl:with-param name="anz" select="$anz - 1" />
        <xsl:with-param name="img" select="$img" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>
  <xsl:include href="MyCoReLayout.xsl" />

</xsl:stylesheet>
