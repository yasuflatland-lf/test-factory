package com.liferay.test.factory.internal.application.list;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.test.factory.constants.TestFactoryPortletKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"panel.app.order:Integer=400",
		"panel.category.key=" + PanelCategoryKeys.CONTROL_PANEL_CONFIGURATION
	},
	service = PanelApp.class
)
public class TestFactoryPanelApp extends BasePanelApp {

	@Override
	public Portlet getPortlet() {
		return _portlet;
	}

	@Override
	public String getPortletId() {
		return TestFactoryPortletKeys.TEST_FACTORY;
	}

	@Reference(
		target = "(javax.portlet.name=" +
			TestFactoryPortletKeys.TEST_FACTORY + ")"
	)
	private Portlet _portlet;

}
