<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.js" />

<body id="siteSettings" jahia-parse-html="true" class="nodesAndTypesLoaded">
<div jahiatype="mainmodule" path="/settings" locale="en" template="manageMemory" templatename="Healthcheck management" nodetypes="nt:base">
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-12">

                <div class="page-header">
                    <h2>Healthcheck panel</h2>
                </div>



                <div class="panel-group" id="accordion2">
                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <a data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
                                <strong>List of allowed tokens</strong>
                            </a>
                        </div>
                        <div id="collapseOne" class="panel-collapse collapse in" role="tabpanel">
                            <div class="panel-body">
                                <table class="table table-striped table-bordered table-hover">
                                    <tbody>
                                    <jcr:node path="/settings/healthcheckSettings" var="healthcheckSettings" />
                                    <c:forEach items="${healthcheckSettings.properties.tokens}" varStatus="status" var="token">

                                    <tr>
                                        <td>
                                            <strong title="The amount of used memory in bytes">Token #${status.index}</strong>
                                        </td>
                                        <td>
                                                ${token.string} ( <a href="#" onclick="javascript:$.get( '${url.server}/en/sites/systemsite.removeHealthcheckToken.do?token=${token.string}', function( data ) { location.reload(); });">-</a> )
                                        </td>
                                        <td>
                                            <a target="_blank" href="${url.server}/healthcheck?token=${token.string}">${url.server}/healthcheck?token=${token.string}</a>
                                        </td>
                                    </tr>
                                    </c:forEach>

                                    </tbody></table>
                            </div>
                        </div>
                    </div>
                    <a href="#" onclick="javascript:$.get( '${url.server}/en/sites/systemsite.addHealthcheckToken.do', function( data ) { location.reload(); });">Generate new token</a>

                </div><div class="clear"></div>
            </div>
        </div>
    </div>








</div>
<div id="snackbar-container"></div></body>








