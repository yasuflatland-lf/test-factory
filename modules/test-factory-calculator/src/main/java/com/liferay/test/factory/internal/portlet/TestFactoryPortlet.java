package com.liferay.test.factory.internal.portlet;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.test.factory.constants.TestFactoryPortletKeys;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"com.liferay.portlet.add-default-resource=true",
		"com.liferay.portlet.css-class-wrapper=portlet-test-factory",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"javax.portlet.display-name=Test Factory Calculator",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + TestFactoryPortletKeys.TEST_FACTORY,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.version=3.0"
	},
	service = Portlet.class
)
public class TestFactoryPortlet extends MVCPortlet {
}
