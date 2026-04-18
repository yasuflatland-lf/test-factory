<%-- NOTE: DXP 2026 の Provide-Capability は http://xmlns.jcp.org/portlet_3_0 のみ広告。
     jakarta.tags.portlet への切替はバンドル解決で死ぬ。変更禁止。
     see docs/ADR/adr-0008-dxp-2026-migration.md --%>
<%@ taglib uri="http://xmlns.jcp.org/portlet_3_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/react" prefix="react" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>

<%@ page import="com.liferay.portal.kernel.json.JSONFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.json.JSONObject" %>
<%@ page import="com.liferay.portal.kernel.util.HashMapBuilder" %>

<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.ResourceBundle" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />
