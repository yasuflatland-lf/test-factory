package com.liferay.support.tools.service;

import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.image.ImageRequest;
import com.liferay.support.tools.service.image.ImageSource;
import com.liferay.support.tools.utils.JournalUtils;
import com.liferay.support.tools.utils.RandomizeContentGenerator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WebContentCreator.class)
public class WebContentCreator {

	public JSONObject createSimple(
			long userId, long[] groupIds, BatchSpec spec, String baseArticle,
			long folderId, String[] locales, boolean neverExpire,
			boolean neverReview)
		throws Throwable {

		int count = spec.count();
		String baseName = spec.baseName();

		List<PerSiteResult> perSiteResults = new ArrayList<>();

		for (long groupId : groupIds) {
			PerSiteResult siteResult = _createSimpleForSite(
				userId, groupId, count, baseName, baseArticle, folderId,
				locales, neverExpire, neverReview);

			perSiteResults.add(siteResult);
		}

		return _buildResponse(count, groupIds.length, perSiteResults);
	}

	public JSONObject createDummy(
			long userId, long[] groupIds, BatchSpec spec, long folderId,
			String[] locales, int titleWords, int totalParagraphs,
			int randomAmount, String linkLists, boolean neverExpire,
			boolean neverReview)
		throws Throwable {

		int count = spec.count();
		String baseName = spec.baseName();

		List<PerSiteResult> perSiteResults = new ArrayList<>();

		for (long groupId : groupIds) {
			PerSiteResult siteResult = _createDummyForSite(
				userId, groupId, count, baseName, folderId, locales, titleWords,
				totalParagraphs, randomAmount, linkLists, neverExpire,
				neverReview);

			perSiteResults.add(siteResult);
		}

		return _buildResponse(count, groupIds.length, perSiteResults);
	}

	public JSONObject createWithStructureTemplate(
			long userId, long[] groupIds, BatchSpec spec, long folderId,
			String[] locales, long ddmStructureId, long ddmTemplateId,
			boolean neverExpire, boolean neverReview)
		throws Throwable {

		int count = spec.count();
		String baseName = spec.baseName();

		List<PerSiteResult> perSiteResults = new ArrayList<>();

		for (long groupId : groupIds) {
			PerSiteResult siteResult = _createWithStructureTemplateForSite(
				userId, groupId, count, baseName, folderId, locales,
				ddmStructureId, ddmTemplateId, neverExpire, neverReview);

			perSiteResults.add(siteResult);
		}

		return _buildResponse(count, groupIds.length, perSiteResults);
	}

	static String resolveImageLinks(
		RandomizeContentGenerator generator, ImageSource imageSource,
		String linkLists, int randomAmount) {

		String safeLinkLists = (linkLists == null) ? "" : linkLists;

		if (randomAmount <= 0) {
			return safeLinkLists;
		}

		List<String> userLinks = safeLinkLists.isBlank() ?
			Collections.emptyList() : generator.generateLinks(safeLinkLists);

		if (userLinks.size() >= randomAmount) {
			return safeLinkLists;
		}

		List<String> merged = new ArrayList<>(userLinks);

		merged.addAll(
			imageSource.supply(
				ImageRequest.of(randomAmount - userLinks.size())));

		return String.join(LDFPortletKeys.EOL, merged);
	}

	private JSONObject _buildResponse(
		int count, int groupCount, List<PerSiteResult> perSiteResults) {

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray perSite = JSONFactoryUtil.createJSONArray();

		int totalCreated = 0;
		boolean allOk = true;

		for (PerSiteResult siteResult : perSiteResults) {
			JSONObject entry = JSONFactoryUtil.createJSONObject();

			entry.put("groupId", siteResult.groupId());
			entry.put("siteName", siteResult.siteName());
			entry.put("created", siteResult.created());
			entry.put("failed", siteResult.failed());

			if (siteResult.error() != null) {
				entry.put("error", siteResult.error());
			}

			totalCreated += siteResult.created();

			if ((siteResult.failed() > 0) || (siteResult.error() != null)) {
				allOk = false;
			}

			perSite.put(entry);
		}

		result.put("ok", allOk);
		result.put("totalRequested", count * groupCount);
		result.put("totalCreated", totalCreated);
		result.put("perSite", perSite);

		return result;
	}

	private PerSiteResult _createSimpleForSite(
		long userId, long groupId, int count, String baseName,
		String baseArticle, long folderId, String[] locales,
		boolean neverExpire, boolean neverReview) {

		String siteName = _resolveSiteName(groupId);
		int created = 0;

		Locale defaultLocale = _resolveDefaultLocale(locales);

		DDMStructure ddmStructure =
			_ddmStructureLocalService.fetchStructure(
				groupId,
				PortalUtil.getClassNameId(JournalArticle.class),
				LDFPortletKeys.DDM_STRUCTURE_KEY);

		if (ddmStructure == null) {
			return new PerSiteResult(
				groupId, siteName, 0, count,
				"Basic Web Content structure '" +
					LDFPortletKeys.DDM_STRUCTURE_KEY +
						"' not found for group " + groupId);
		}

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			defaultLocale, baseName);

		for (int i = 0; i < count; i++) {
			final String title = BatchNaming.resolve(baseName, count, i);
			final Map<Locale, String> titleMap = Collections.singletonMap(
				defaultLocale, title);

			try {
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> {
						String content = _journalUtils.buildFields(
							groupId, locales, baseArticle);

						ServiceContext serviceContext = _newServiceContext(
							userId, groupId);

						JournalArticle added =
							_journalArticleLocalService.addArticle(
								null, userId, groupId, folderId, titleMap,
								descriptionMap, content,
								ddmStructure.getStructureId(),
								LDFPortletKeys.DDM_STRUCTURE_KEY,
								serviceContext);

						return _updateArticleLifecycle(
							added, serviceContext, neverExpire, neverReview);
					});

				created++;
			}
			catch (Throwable throwable) {
				if (throwable instanceof Error) {
					throw (Error)throwable;
				}

				_log.error(
					"Failed to create article " + (i + 1) + " of " + count +
						" in group " + groupId,
					throwable);

				return new PerSiteResult(
					groupId, siteName, created, count - created,
					_errorMessage(throwable));
			}
		}

		return new PerSiteResult(groupId, siteName, created, 0, null);
	}

	private PerSiteResult _createDummyForSite(
		long userId, long groupId, int count, String baseName, long folderId,
		String[] locales, int titleWords, int totalParagraphs, int randomAmount,
		String linkLists, boolean neverExpire, boolean neverReview) {

		String siteName = _resolveSiteName(groupId);
		int created = 0;

		Locale defaultLocale = _resolveDefaultLocale(locales);
		String language = defaultLocale.getLanguage();

		DDMStructure ddmStructure =
			_ddmStructureLocalService.fetchStructure(
				groupId,
				PortalUtil.getClassNameId(JournalArticle.class),
				LDFPortletKeys.DDM_STRUCTURE_KEY);

		if (ddmStructure == null) {
			return new PerSiteResult(
				groupId, siteName, 0, count,
				"Basic Web Content structure '" +
					LDFPortletKeys.DDM_STRUCTURE_KEY +
						"' not found for group " + groupId);
		}

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			defaultLocale, baseName);

		for (int i = 0; i < count; i++) {
			String randomTitle =
				_randomizeContentGenerator.generateRandomTitleString(
					language, titleWords);

			if (count > 1) {
				randomTitle = randomTitle + " " + (i + 1);
			}

			final String titleFinal = randomTitle;
			final Map<Locale, String> titleMap = Collections.singletonMap(
				defaultLocale, titleFinal);

			try {
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> {
						String mergedLinks = resolveImageLinks(
							_randomizeContentGenerator, _imageSource,
							linkLists, randomAmount);

						String randomArticle =
							_randomizeContentGenerator.generateRandomContents(
								language, totalParagraphs, randomAmount,
								mergedLinks);

						String content = _journalUtils.buildFields(
							groupId, locales, randomArticle);

						ServiceContext serviceContext = _newServiceContext(
							userId, groupId);

						JournalArticle added =
							_journalArticleLocalService.addArticle(
								null, userId, groupId, folderId, titleMap,
								descriptionMap, content,
								ddmStructure.getStructureId(),
								LDFPortletKeys.DDM_STRUCTURE_KEY,
								serviceContext);

						return _updateArticleLifecycle(
							added, serviceContext, neverExpire, neverReview);
					});

				created++;
			}
			catch (Throwable throwable) {
				if (throwable instanceof Error) {
					throw (Error)throwable;
				}

				_log.error(
					"Failed to create article " + (i + 1) + " of " + count +
						" in group " + groupId,
					throwable);

				return new PerSiteResult(
					groupId, siteName, created, count - created,
					_errorMessage(throwable));
			}
		}

		return new PerSiteResult(groupId, siteName, created, 0, null);
	}

	private PerSiteResult _createWithStructureTemplateForSite(
		long userId, long groupId, int count, String baseName, long folderId,
		String[] locales, long ddmStructureId, long ddmTemplateId,
		boolean neverExpire, boolean neverReview) {

		String siteName = _resolveSiteName(groupId);
		int created = 0;

		Locale defaultLocale = _resolveDefaultLocale(locales);

		final DDMStructure ddmStructure;
		final String templateKey;

		try {
			ddmStructure = _ddmStructureLocalService.getStructure(
				ddmStructureId);

			DDMTemplate ddmTemplate = _ddmTemplateLocalService.getTemplate(
				ddmTemplateId);

			templateKey = ddmTemplate.getTemplateKey();
		}
		catch (Exception exception) {
			return new PerSiteResult(
				groupId, siteName, 0, count, _errorMessage(exception));
		}

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			defaultLocale, baseName);

		for (int i = 0; i < count; i++) {
			final String title = BatchNaming.resolve(baseName, count, i);
			final Map<Locale, String> titleMap = Collections.singletonMap(
				defaultLocale, title);

			try {
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> {
						String content = _journalUtils.buildFields(
							groupId, ddmStructure, locales);

						ServiceContext serviceContext = _newServiceContext(
							userId, groupId);

						JournalArticle added =
							_journalArticleLocalService.addArticle(
								null, userId, groupId, folderId, titleMap,
								descriptionMap, content,
								ddmStructure.getStructureId(), templateKey,
								serviceContext);

						return _updateArticleLifecycle(
							added, serviceContext, neverExpire, neverReview);
					});

				created++;
			}
			catch (Throwable throwable) {
				if (throwable instanceof Error) {
					throw (Error)throwable;
				}

				_log.error(
					"Failed to create article " + (i + 1) + " of " + count +
						" in group " + groupId,
					throwable);

				return new PerSiteResult(
					groupId, siteName, created, count - created,
					_errorMessage(throwable));
			}
		}

		return new PerSiteResult(groupId, siteName, created, 0, null);
	}

	private String _errorMessage(Throwable throwable) {
		String message = throwable.getMessage();

		if ((message != null) && !message.isEmpty()) {
			return message;
		}

		return throwable.getClass().getSimpleName();
	}

	private ServiceContext _newServiceContext(long userId, long groupId)
		throws Exception {

		ServiceContext serviceContext = new ServiceContext();

		User user = _userLocalService.getUser(userId);

		serviceContext.setCompanyId(user.getCompanyId());
		serviceContext.setUserId(userId);
		serviceContext.setScopeGroupId(groupId);
		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAttribute("defaultLanguageId", LocaleUtil.toLanguageId(
			_portal.getSiteDefaultLocale(groupId)));
		serviceContext.setFormDate(new Date());

		return serviceContext;
	}

	private Locale _resolveDefaultLocale(String[] locales) {
		if ((locales != null) && (locales.length > 0) &&
			(locales[0] != null) && !locales[0].isEmpty()) {

			return LocaleUtil.fromLanguageId(locales[0]);
		}

		return LocaleUtil.getDefault();
	}

	private String _resolveSiteName(long groupId) {
		try {
			Group group = _groupLocalService.fetchGroup(groupId);

			if (group != null) {
				return group.getDescriptiveName();
			}
		}
		catch (Exception exception) {
			_log.warn(
				"Unable to resolve descriptive name for group " + groupId +
					", using id instead",
				exception);
		}

		return String.valueOf(groupId);
	}

	private JournalArticle _updateArticleLifecycle(
			JournalArticle createdArticle, ServiceContext serviceContext,
			boolean neverExpire, boolean neverReview)
		throws Exception {

		User user = _userLocalService.getUser(createdArticle.getUserId());

		Date displayDate = createdArticle.getDisplayDate();

		int displayDateMonth = 0;
		int displayDateDay = 0;
		int displayDateYear = 0;
		int displayDateHour = 0;
		int displayDateMinute = 0;

		if (displayDate != null) {
			Calendar displayCal = CalendarFactoryUtil.getCalendar(
				user.getTimeZone());

			displayCal.setTime(displayDate);

			displayDateMonth = displayCal.get(Calendar.MONTH);
			displayDateDay = displayCal.get(Calendar.DATE);
			displayDateYear = displayCal.get(Calendar.YEAR);
			displayDateHour = displayCal.get(Calendar.HOUR);
			displayDateMinute = displayCal.get(Calendar.MINUTE);

			if (displayCal.get(Calendar.AM_PM) == Calendar.PM) {
				displayDateHour += 12;
			}
		}

		Date expirationDate = createdArticle.getExpirationDate();

		int expirationDateMonth = 0;
		int expirationDateDay = 0;
		int expirationDateYear = 0;
		int expirationDateHour = 0;
		int expirationDateMinute = 0;

		if (!neverExpire) {
			Calendar expirationCal = CalendarFactoryUtil.getCalendar(
				user.getTimeZone());

			if (expirationDate == null) {
				expirationCal.setTime(new Date());
				expirationCal.add(Calendar.MONTH, 1);
			}
			else {
				expirationCal.setTime(expirationDate);
			}

			expirationDateMonth = expirationCal.get(Calendar.MONTH);
			expirationDateDay = expirationCal.get(Calendar.DATE);
			expirationDateYear = expirationCal.get(Calendar.YEAR);
			expirationDateHour = expirationCal.get(Calendar.HOUR);
			expirationDateMinute = expirationCal.get(Calendar.MINUTE);

			if (expirationCal.get(Calendar.AM_PM) == Calendar.PM) {
				expirationDateHour += 12;
			}
		}

		Date reviewDate = createdArticle.getReviewDate();

		int reviewDateMonth = 0;
		int reviewDateDay = 0;
		int reviewDateYear = 0;
		int reviewDateHour = 0;
		int reviewDateMinute = 0;

		if (!neverReview) {
			Calendar reviewCal = CalendarFactoryUtil.getCalendar(
				user.getTimeZone());

			if (reviewDate == null) {
				reviewCal.setTime(new Date());
				reviewCal.add(Calendar.MONTH, 1);
			}
			else {
				reviewCal.setTime(reviewDate);
			}

			reviewDateMonth = reviewCal.get(Calendar.MONTH);
			reviewDateDay = reviewCal.get(Calendar.DATE);
			reviewDateYear = reviewCal.get(Calendar.YEAR);
			reviewDateHour = reviewCal.get(Calendar.HOUR);
			reviewDateMinute = reviewCal.get(Calendar.MINUTE);

			if (reviewCal.get(Calendar.AM_PM) == Calendar.PM) {
				reviewDateHour += 12;
			}
		}

		serviceContext.setFormDate(new Date());

		return _journalArticleLocalService.updateArticle(
			createdArticle.getUserId(), createdArticle.getGroupId(),
			createdArticle.getFolderId(), createdArticle.getArticleId(),
			createdArticle.getVersion(), createdArticle.getTitleMap(),
			createdArticle.getDescriptionMap(),
			createdArticle.getFriendlyURLMap(), createdArticle.getContent(),
			createdArticle.getDDMTemplateKey(), createdArticle.getLayoutUuid(),
			displayDateMonth, displayDateDay, displayDateYear, displayDateHour,
			displayDateMinute, expirationDateMonth, expirationDateDay,
			expirationDateYear, expirationDateHour, expirationDateMinute,
			neverExpire, reviewDateMonth, reviewDateDay, reviewDateYear,
			reviewDateHour, reviewDateMinute, neverReview,
			createdArticle.isIndexable(), createdArticle.isSmallImage(),
			createdArticle.getSmallImageId(),
			createdArticle.getSmallImageSource(),
			createdArticle.getSmallImageURL(), null, null, null,
			serviceContext);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		WebContentCreator.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private DDMStructureLocalService _ddmStructureLocalService;

	@Reference
	private DDMTemplateLocalService _ddmTemplateLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private ImageSource _imageSource;

	@Reference
	private JournalArticleLocalService _journalArticleLocalService;

	@Reference
	private JournalUtils _journalUtils;

	@Reference
	private Portal _portal;

	@Reference
	private RandomizeContentGenerator _randomizeContentGenerator;

	@Reference
	private UserLocalService _userLocalService;

	private record PerSiteResult(
		long groupId, String siteName, int created, int failed, String error) {

		PerSiteResult {
			if ((created < 0) || (failed < 0)) {
				throw new IllegalArgumentException(
					"created and failed must be non-negative");
			}

			if (siteName == null) {
				throw new IllegalArgumentException("siteName is required");
			}

			if ((error != null) && (failed == 0)) {
				throw new IllegalArgumentException(
					"error requires failed > 0");
			}
		}

	}

}
