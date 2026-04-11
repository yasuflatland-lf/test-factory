package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.WebContentCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/wcm"
	},
	service = MVCResourceCommand.class
)
public class WcmResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String dataString = ParamUtil.getString(
			httpServletRequest, "data");

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		try {
			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			long[] groupIds = _parseGroupIds(data);

			if (groupIds.length == 0) {
				throw new IllegalArgumentException(
					"groupIds must contain at least one positive group id");
			}

			long folderId = GetterUtil.getLong(data.getString("folderId"));
			String[] locales = _parseLocales(
				data.getString("locales"), groupIds[0]);
			boolean neverExpire = GetterUtil.getBoolean(
				data.getString("neverExpire"), true);
			boolean neverReview = GetterUtil.getBoolean(
				data.getString("neverReview"), true);

			int createContentsType = GetterUtil.getInteger(
				data.getString("createContentsType"));

			long userId = _portal.getUserId(resourceRequest);

			if (createContentsType == 0) {
				String baseArticle = GetterUtil.getString(
					data.getString("baseArticle"));

				responseJson = _webContentCreator.createSimple(
					userId, groupIds, batchSpec, baseArticle, folderId, locales,
					neverExpire, neverReview);
			}
			else if (createContentsType == 1) {
				int titleWords = GetterUtil.getInteger(
					data.getString("titleWords"), 5);
				int totalParagraphs = GetterUtil.getInteger(
					data.getString("totalParagraphs"), 3);
				int randomAmount = GetterUtil.getInteger(
					data.getString("randomAmount"), 3);
				String linkLists = GetterUtil.getString(
					data.getString("linkLists"));

				responseJson = _webContentCreator.createDummy(
					userId, groupIds, batchSpec, folderId, locales, titleWords,
					totalParagraphs, randomAmount, linkLists, neverExpire,
					neverReview);
			}
			else if (createContentsType == 2) {
				long ddmStructureId = GetterUtil.getLong(
					data.getString("ddmStructureId"));
				long ddmTemplateId = GetterUtil.getLong(
					data.getString("ddmTemplateId"));

				if (ddmStructureId <= 0) {
					throw new IllegalArgumentException(
						"ddmStructureId must be a positive number");
				}

				if (ddmTemplateId <= 0) {
					throw new IllegalArgumentException(
						"ddmTemplateId must be a positive number");
				}

				responseJson = _webContentCreator.createWithStructureTemplate(
					userId, groupIds, batchSpec, folderId, locales,
					ddmStructureId, ddmTemplateId, neverExpire, neverReview);
			}
			else {
				throw new IllegalArgumentException(
					"Unknown createContentsType: " + createContentsType);
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create web contents", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private long[] _parseGroupIds(JSONObject data) {
		JSONArray jsonArray = data.getJSONArray("groupIds");

		if ((jsonArray != null) && (jsonArray.length() > 0)) {
			List<Long> parsed = new ArrayList<>();

			for (int i = 0; i < jsonArray.length(); i++) {
				long value = GetterUtil.getLong(jsonArray.get(i));

				if (value > 0) {
					parsed.add(value);
				}
			}

			return _toLongArray(parsed);
		}

		String groupIdsString = data.getString("groupIds");

		if (Validator.isNotNull(groupIdsString)) {
			List<Long> parsed = new ArrayList<>();

			for (String token : groupIdsString.split(",")) {
				if ((token != null) && !token.trim().isEmpty()) {
					long value = GetterUtil.getLong(token.trim());

					if (value > 0) {
						parsed.add(value);
					}
				}
			}

			return _toLongArray(parsed);
		}

		long legacyGroupId = GetterUtil.getLong(data.getString("groupId"));

		if (legacyGroupId > 0) {
			return new long[] {legacyGroupId};
		}

		return new long[0];
	}

	private String[] _parseLocales(String localesCsv, long groupId) {
		if (Validator.isNotNull(localesCsv)) {
			String[] tokens = localesCsv.split(",");
			int nonEmpty = 0;

			for (String token : tokens) {
				if ((token != null) && !token.trim().isEmpty()) {
					nonEmpty++;
				}
			}

			if (nonEmpty > 0) {
				String[] result = new String[nonEmpty];
				int idx = 0;

				for (String token : tokens) {
					if ((token != null) && !token.trim().isEmpty()) {
						result[idx++] = token.trim();
					}
				}

				return result;
			}
		}

		try {
			Locale defaultLocale = _portal.getSiteDefaultLocale(groupId);

			return new String[] {LocaleUtil.toLanguageId(defaultLocale)};
		}
		catch (PortalException portalException) {
			_log.warn(
				"Unable to resolve site default locale for groupId " +
					groupId + ", falling back to portal default",
				portalException);

			return new String[] {
				LocaleUtil.toLanguageId(LocaleUtil.getDefault())
			};
		}
	}

	private long[] _toLongArray(List<Long> values) {
		long[] result = new long[values.size()];

		for (int i = 0; i < values.size(); i++) {
			result[i] = values.get(i);
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		WcmResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private WebContentCreator _webContentCreator;

}
