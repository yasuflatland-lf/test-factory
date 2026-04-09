/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service.persistence.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.test.factory.exception.NoSuchCalcEntryException;
import com.liferay.test.factory.model.CalcEntry;
import com.liferay.test.factory.model.CalcEntryTable;
import com.liferay.test.factory.model.impl.CalcEntryImpl;
import com.liferay.test.factory.model.impl.CalcEntryModelImpl;
import com.liferay.test.factory.service.persistence.CalcEntryPersistence;
import com.liferay.test.factory.service.persistence.CalcEntryUtil;
import com.liferay.test.factory.service.persistence.impl.constants.TestFactoryPersistenceConstants;

import java.io.Serializable;

import java.lang.reflect.InvocationHandler;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The persistence implementation for the calc entry service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
@Component(service = CalcEntryPersistence.class)
public class CalcEntryPersistenceImpl
	extends BasePersistenceImpl<CalcEntry> implements CalcEntryPersistence {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use <code>CalcEntryUtil</code> to access the calc entry persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static final String FINDER_CLASS_NAME_ENTITY =
		CalcEntryImpl.class.getName();

	public static final String FINDER_CLASS_NAME_LIST_WITH_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List1";

	public static final String FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List2";

	private FinderPath _finderPathWithPaginationFindAll;
	private FinderPath _finderPathWithoutPaginationFindAll;
	private FinderPath _finderPathCountAll;
	private FinderPath _finderPathWithPaginationFindByUserId;
	private FinderPath _finderPathWithoutPaginationFindByUserId;
	private FinderPath _finderPathCountByUserId;

	/**
	 * Returns all the calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the matching calc entries
	 */
	@Override
	public List<CalcEntry> findByUserId(long userId) {
		return findByUserId(userId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	/**
	 * Returns a range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @return the range of matching calc entries
	 */
	@Override
	public List<CalcEntry> findByUserId(long userId, int start, int end) {
		return findByUserId(userId, start, end, null);
	}

	/**
	 * Returns an ordered range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching calc entries
	 */
	@Override
	public List<CalcEntry> findByUserId(
		long userId, int start, int end,
		OrderByComparator<CalcEntry> orderByComparator) {

		return findByUserId(userId, start, end, orderByComparator, true);
	}

	/**
	 * Returns an ordered range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching calc entries
	 */
	@Override
	public List<CalcEntry> findByUserId(
		long userId, int start, int end,
		OrderByComparator<CalcEntry> orderByComparator,
		boolean useFinderCache) {

		FinderPath finderPath = null;
		Object[] finderArgs = null;

		if ((start == QueryUtil.ALL_POS) && (end == QueryUtil.ALL_POS) &&
			(orderByComparator == null)) {

			if (useFinderCache) {
				finderPath = _finderPathWithoutPaginationFindByUserId;
				finderArgs = new Object[] {userId};
			}
		}
		else if (useFinderCache) {
			finderPath = _finderPathWithPaginationFindByUserId;
			finderArgs = new Object[] {userId, start, end, orderByComparator};
		}

		List<CalcEntry> list = null;

		if (useFinderCache) {
			list = (List<CalcEntry>)finderCache.getResult(
				finderPath, finderArgs, this);

			if ((list != null) && !list.isEmpty()) {
				for (CalcEntry calcEntry : list) {
					if (userId != calcEntry.getUserId()) {
						list = null;

						break;
					}
				}
			}
		}

		if (list == null) {
			StringBundler sb = null;

			if (orderByComparator != null) {
				sb = new StringBundler(
					3 + (orderByComparator.getOrderByFields().length * 2));
			}
			else {
				sb = new StringBundler(3);
			}

			sb.append(_SQL_SELECT_CALCENTRY_WHERE);

			sb.append(_FINDER_COLUMN_USERID_USERID_2);

			if (orderByComparator != null) {
				appendOrderByComparator(
					sb, _ORDER_BY_ENTITY_ALIAS, orderByComparator);
			}
			else {
				sb.append(CalcEntryModelImpl.ORDER_BY_JPQL);
			}

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(userId);

				list = (List<CalcEntry>)QueryUtil.list(
					query, getDialect(), start, end);

				cacheResult(list);

				if (useFinderCache) {
					finderCache.putResult(finderPath, finderArgs, list);
				}
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	@Override
	public CalcEntry findByUserId_First(
			long userId, OrderByComparator<CalcEntry> orderByComparator)
		throws NoSuchCalcEntryException {

		CalcEntry calcEntry = fetchByUserId_First(userId, orderByComparator);

		if (calcEntry != null) {
			return calcEntry;
		}

		StringBundler sb = new StringBundler(4);

		sb.append(_NO_SUCH_ENTITY_WITH_KEY);

		sb.append("userId=");
		sb.append(userId);

		sb.append("}");

		throw new NoSuchCalcEntryException(sb.toString());
	}

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	@Override
	public CalcEntry fetchByUserId_First(
		long userId, OrderByComparator<CalcEntry> orderByComparator) {

		List<CalcEntry> list = findByUserId(userId, 0, 1, orderByComparator);

		if (!list.isEmpty()) {
			return list.get(0);
		}

		return null;
	}

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	@Override
	public CalcEntry findByUserId_Last(
			long userId, OrderByComparator<CalcEntry> orderByComparator)
		throws NoSuchCalcEntryException {

		CalcEntry calcEntry = fetchByUserId_Last(userId, orderByComparator);

		if (calcEntry != null) {
			return calcEntry;
		}

		StringBundler sb = new StringBundler(4);

		sb.append(_NO_SUCH_ENTITY_WITH_KEY);

		sb.append("userId=");
		sb.append(userId);

		sb.append("}");

		throw new NoSuchCalcEntryException(sb.toString());
	}

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	@Override
	public CalcEntry fetchByUserId_Last(
		long userId, OrderByComparator<CalcEntry> orderByComparator) {

		int count = countByUserId(userId);

		if (count == 0) {
			return null;
		}

		List<CalcEntry> list = findByUserId(
			userId, count - 1, count, orderByComparator);

		if (!list.isEmpty()) {
			return list.get(0);
		}

		return null;
	}

	/**
	 * Returns the calc entries before and after the current calc entry in the ordered set where userId = &#63;.
	 *
	 * @param calcEntryId the primary key of the current calc entry
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the previous, current, and next calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry[] findByUserId_PrevAndNext(
			long calcEntryId, long userId,
			OrderByComparator<CalcEntry> orderByComparator)
		throws NoSuchCalcEntryException {

		CalcEntry calcEntry = findByPrimaryKey(calcEntryId);

		Session session = null;

		try {
			session = openSession();

			CalcEntry[] array = new CalcEntryImpl[3];

			array[0] = getByUserId_PrevAndNext(
				session, calcEntry, userId, orderByComparator, true);

			array[1] = calcEntry;

			array[2] = getByUserId_PrevAndNext(
				session, calcEntry, userId, orderByComparator, false);

			return array;
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}
	}

	protected CalcEntry getByUserId_PrevAndNext(
		Session session, CalcEntry calcEntry, long userId,
		OrderByComparator<CalcEntry> orderByComparator, boolean previous) {

		StringBundler sb = null;

		if (orderByComparator != null) {
			sb = new StringBundler(
				4 + (orderByComparator.getOrderByConditionFields().length * 3) +
					(orderByComparator.getOrderByFields().length * 3));
		}
		else {
			sb = new StringBundler(3);
		}

		sb.append(_SQL_SELECT_CALCENTRY_WHERE);

		sb.append(_FINDER_COLUMN_USERID_USERID_2);

		if (orderByComparator != null) {
			String[] orderByConditionFields =
				orderByComparator.getOrderByConditionFields();

			if (orderByConditionFields.length > 0) {
				sb.append(WHERE_AND);
			}

			for (int i = 0; i < orderByConditionFields.length; i++) {
				sb.append(_ORDER_BY_ENTITY_ALIAS);
				sb.append(orderByConditionFields[i]);

				if ((i + 1) < orderByConditionFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(WHERE_GREATER_THAN_HAS_NEXT);
					}
					else {
						sb.append(WHERE_LESSER_THAN_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(WHERE_GREATER_THAN);
					}
					else {
						sb.append(WHERE_LESSER_THAN);
					}
				}
			}

			sb.append(ORDER_BY_CLAUSE);

			String[] orderByFields = orderByComparator.getOrderByFields();

			for (int i = 0; i < orderByFields.length; i++) {
				sb.append(_ORDER_BY_ENTITY_ALIAS);
				sb.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(ORDER_BY_ASC_HAS_NEXT);
					}
					else {
						sb.append(ORDER_BY_DESC_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(ORDER_BY_ASC);
					}
					else {
						sb.append(ORDER_BY_DESC);
					}
				}
			}
		}
		else {
			sb.append(CalcEntryModelImpl.ORDER_BY_JPQL);
		}

		String sql = sb.toString();

		Query query = session.createQuery(sql);

		query.setFirstResult(0);
		query.setMaxResults(2);

		QueryPos queryPos = QueryPos.getInstance(query);

		queryPos.add(userId);

		if (orderByComparator != null) {
			for (Object orderByConditionValue :
					orderByComparator.getOrderByConditionValues(calcEntry)) {

				queryPos.add(orderByConditionValue);
			}
		}

		List<CalcEntry> list = query.list();

		if (list.size() == 2) {
			return list.get(1);
		}
		else {
			return null;
		}
	}

	/**
	 * Removes all the calc entries where userId = &#63; from the database.
	 *
	 * @param userId the user ID
	 */
	@Override
	public void removeByUserId(long userId) {
		for (CalcEntry calcEntry :
				findByUserId(
					userId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)) {

			remove(calcEntry);
		}
	}

	/**
	 * Returns the number of calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the number of matching calc entries
	 */
	@Override
	public int countByUserId(long userId) {
		FinderPath finderPath = _finderPathCountByUserId;

		Object[] finderArgs = new Object[] {userId};

		Long count = (Long)finderCache.getResult(finderPath, finderArgs, this);

		if (count == null) {
			StringBundler sb = new StringBundler(2);

			sb.append(_SQL_COUNT_CALCENTRY_WHERE);

			sb.append(_FINDER_COLUMN_USERID_USERID_2);

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(userId);

				count = (Long)query.uniqueResult();

				finderCache.putResult(finderPath, finderArgs, count);
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	private static final String _FINDER_COLUMN_USERID_USERID_2 =
		"calcEntry.userId = ?";

	public CalcEntryPersistenceImpl() {
		setModelClass(CalcEntry.class);

		setModelImplClass(CalcEntryImpl.class);
		setModelPKClass(long.class);

		setTable(CalcEntryTable.INSTANCE);
	}

	/**
	 * Caches the calc entry in the entity cache if it is enabled.
	 *
	 * @param calcEntry the calc entry
	 */
	@Override
	public void cacheResult(CalcEntry calcEntry) {
		entityCache.putResult(
			CalcEntryImpl.class, calcEntry.getPrimaryKey(), calcEntry);
	}

	private int _valueObjectFinderCacheListThreshold;

	/**
	 * Caches the calc entries in the entity cache if it is enabled.
	 *
	 * @param calcEntries the calc entries
	 */
	@Override
	public void cacheResult(List<CalcEntry> calcEntries) {
		if ((_valueObjectFinderCacheListThreshold == 0) ||
			((_valueObjectFinderCacheListThreshold > 0) &&
			 (calcEntries.size() > _valueObjectFinderCacheListThreshold))) {

			return;
		}

		for (CalcEntry calcEntry : calcEntries) {
			if (entityCache.getResult(
					CalcEntryImpl.class, calcEntry.getPrimaryKey()) == null) {

				cacheResult(calcEntry);
			}
		}
	}

	/**
	 * Clears the cache for all calc entries.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache() {
		entityCache.clearCache(CalcEntryImpl.class);

		finderCache.clearCache(CalcEntryImpl.class);
	}

	/**
	 * Clears the cache for the calc entry.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache(CalcEntry calcEntry) {
		entityCache.removeResult(CalcEntryImpl.class, calcEntry);
	}

	@Override
	public void clearCache(List<CalcEntry> calcEntries) {
		for (CalcEntry calcEntry : calcEntries) {
			entityCache.removeResult(CalcEntryImpl.class, calcEntry);
		}
	}

	@Override
	public void clearCache(Set<Serializable> primaryKeys) {
		finderCache.clearCache(CalcEntryImpl.class);

		for (Serializable primaryKey : primaryKeys) {
			entityCache.removeResult(CalcEntryImpl.class, primaryKey);
		}
	}

	/**
	 * Creates a new calc entry with the primary key. Does not add the calc entry to the database.
	 *
	 * @param calcEntryId the primary key for the new calc entry
	 * @return the new calc entry
	 */
	@Override
	public CalcEntry create(long calcEntryId) {
		CalcEntry calcEntry = new CalcEntryImpl();

		calcEntry.setNew(true);
		calcEntry.setPrimaryKey(calcEntryId);

		calcEntry.setCompanyId(CompanyThreadLocal.getCompanyId());

		return calcEntry;
	}

	/**
	 * Removes the calc entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry that was removed
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry remove(long calcEntryId) throws NoSuchCalcEntryException {
		return remove((Serializable)calcEntryId);
	}

	/**
	 * Removes the calc entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param primaryKey the primary key of the calc entry
	 * @return the calc entry that was removed
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry remove(Serializable primaryKey)
		throws NoSuchCalcEntryException {

		Session session = null;

		try {
			session = openSession();

			CalcEntry calcEntry = (CalcEntry)session.get(
				CalcEntryImpl.class, primaryKey);

			if (calcEntry == null) {
				if (_log.isDebugEnabled()) {
					_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
				}

				throw new NoSuchCalcEntryException(
					_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			return remove(calcEntry);
		}
		catch (NoSuchCalcEntryException noSuchEntityException) {
			throw noSuchEntityException;
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}
	}

	@Override
	protected CalcEntry removeImpl(CalcEntry calcEntry) {
		Session session = null;

		try {
			session = openSession();

			if (!session.contains(calcEntry)) {
				calcEntry = (CalcEntry)session.get(
					CalcEntryImpl.class, calcEntry.getPrimaryKeyObj());
			}

			if (calcEntry != null) {
				session.delete(calcEntry);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		if (calcEntry != null) {
			clearCache(calcEntry);
		}

		return calcEntry;
	}

	@Override
	public CalcEntry updateImpl(CalcEntry calcEntry) {
		boolean isNew = calcEntry.isNew();

		if (!(calcEntry instanceof CalcEntryModelImpl)) {
			InvocationHandler invocationHandler = null;

			if (ProxyUtil.isProxyClass(calcEntry.getClass())) {
				invocationHandler = ProxyUtil.getInvocationHandler(calcEntry);

				throw new IllegalArgumentException(
					"Implement ModelWrapper in calcEntry proxy " +
						invocationHandler.getClass());
			}

			throw new IllegalArgumentException(
				"Implement ModelWrapper in custom CalcEntry implementation " +
					calcEntry.getClass());
		}

		CalcEntryModelImpl calcEntryModelImpl = (CalcEntryModelImpl)calcEntry;

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		Date date = new Date();

		if (isNew && (calcEntry.getCreateDate() == null)) {
			if (serviceContext == null) {
				calcEntry.setCreateDate(date);
			}
			else {
				calcEntry.setCreateDate(serviceContext.getCreateDate(date));
			}
		}

		if (!calcEntryModelImpl.hasSetModifiedDate()) {
			if (serviceContext == null) {
				calcEntry.setModifiedDate(date);
			}
			else {
				calcEntry.setModifiedDate(serviceContext.getModifiedDate(date));
			}
		}

		Session session = null;

		try {
			session = openSession();

			if (isNew) {
				session.save(calcEntry);
			}
			else {
				calcEntry = (CalcEntry)session.merge(calcEntry);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		entityCache.putResult(
			CalcEntryImpl.class, calcEntryModelImpl, false, true);

		if (isNew) {
			calcEntry.setNew(false);
		}

		calcEntry.resetOriginalValues();

		return calcEntry;
	}

	/**
	 * Returns the calc entry with the primary key or throws a <code>com.liferay.portal.kernel.exception.NoSuchModelException</code> if it could not be found.
	 *
	 * @param primaryKey the primary key of the calc entry
	 * @return the calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry findByPrimaryKey(Serializable primaryKey)
		throws NoSuchCalcEntryException {

		CalcEntry calcEntry = fetchByPrimaryKey(primaryKey);

		if (calcEntry == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			throw new NoSuchCalcEntryException(
				_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
		}

		return calcEntry;
	}

	/**
	 * Returns the calc entry with the primary key or throws a <code>NoSuchCalcEntryException</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry findByPrimaryKey(long calcEntryId)
		throws NoSuchCalcEntryException {

		return findByPrimaryKey((Serializable)calcEntryId);
	}

	/**
	 * Returns the calc entry with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry, or <code>null</code> if a calc entry with the primary key could not be found
	 */
	@Override
	public CalcEntry fetchByPrimaryKey(long calcEntryId) {
		return fetchByPrimaryKey((Serializable)calcEntryId);
	}

	/**
	 * Returns all the calc entries.
	 *
	 * @return the calc entries
	 */
	@Override
	public List<CalcEntry> findAll() {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	/**
	 * Returns a range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @return the range of calc entries
	 */
	@Override
	public List<CalcEntry> findAll(int start, int end) {
		return findAll(start, end, null);
	}

	/**
	 * Returns an ordered range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of calc entries
	 */
	@Override
	public List<CalcEntry> findAll(
		int start, int end, OrderByComparator<CalcEntry> orderByComparator) {

		return findAll(start, end, orderByComparator, true);
	}

	/**
	 * Returns an ordered range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of calc entries
	 */
	@Override
	public List<CalcEntry> findAll(
		int start, int end, OrderByComparator<CalcEntry> orderByComparator,
		boolean useFinderCache) {

		FinderPath finderPath = null;
		Object[] finderArgs = null;

		if ((start == QueryUtil.ALL_POS) && (end == QueryUtil.ALL_POS) &&
			(orderByComparator == null)) {

			if (useFinderCache) {
				finderPath = _finderPathWithoutPaginationFindAll;
				finderArgs = FINDER_ARGS_EMPTY;
			}
		}
		else if (useFinderCache) {
			finderPath = _finderPathWithPaginationFindAll;
			finderArgs = new Object[] {start, end, orderByComparator};
		}

		List<CalcEntry> list = null;

		if (useFinderCache) {
			list = (List<CalcEntry>)finderCache.getResult(
				finderPath, finderArgs, this);
		}

		if (list == null) {
			StringBundler sb = null;
			String sql = null;

			if (orderByComparator != null) {
				sb = new StringBundler(
					2 + (orderByComparator.getOrderByFields().length * 2));

				sb.append(_SQL_SELECT_CALCENTRY);

				appendOrderByComparator(
					sb, _ORDER_BY_ENTITY_ALIAS, orderByComparator);

				sql = sb.toString();
			}
			else {
				sql = _SQL_SELECT_CALCENTRY;

				sql = sql.concat(CalcEntryModelImpl.ORDER_BY_JPQL);
			}

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				list = (List<CalcEntry>)QueryUtil.list(
					query, getDialect(), start, end);

				cacheResult(list);

				if (useFinderCache) {
					finderCache.putResult(finderPath, finderArgs, list);
				}
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Removes all the calc entries from the database.
	 *
	 */
	@Override
	public void removeAll() {
		for (CalcEntry calcEntry : findAll()) {
			remove(calcEntry);
		}
	}

	/**
	 * Returns the number of calc entries.
	 *
	 * @return the number of calc entries
	 */
	@Override
	public int countAll() {
		Long count = (Long)finderCache.getResult(
			_finderPathCountAll, FINDER_ARGS_EMPTY, this);

		if (count == null) {
			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(_SQL_COUNT_CALCENTRY);

				count = (Long)query.uniqueResult();

				finderCache.putResult(
					_finderPathCountAll, FINDER_ARGS_EMPTY, count);
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	@Override
	protected EntityCache getEntityCache() {
		return entityCache;
	}

	@Override
	protected String getPKDBName() {
		return "calcEntryId";
	}

	@Override
	protected String getSelectSQL() {
		return _SQL_SELECT_CALCENTRY;
	}

	@Override
	protected Map<String, Integer> getTableColumnsMap() {
		return CalcEntryModelImpl.TABLE_COLUMNS_MAP;
	}

	/**
	 * Initializes the calc entry persistence.
	 */
	@Activate
	public void activate() {
		_valueObjectFinderCacheListThreshold = GetterUtil.getInteger(
			PropsUtil.get(PropsKeys.VALUE_OBJECT_FINDER_CACHE_LIST_THRESHOLD));

		_finderPathWithPaginationFindAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findAll", new String[0],
			new String[0], true);

		_finderPathWithoutPaginationFindAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findAll", new String[0],
			new String[0], true);

		_finderPathCountAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countAll",
			new String[0], new String[0], false);

		_finderPathWithPaginationFindByUserId = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findByUserId",
			new String[] {
				Long.class.getName(), Integer.class.getName(),
				Integer.class.getName(), OrderByComparator.class.getName()
			},
			new String[] {"userId"}, true);

		_finderPathWithoutPaginationFindByUserId = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findByUserId",
			new String[] {Long.class.getName()}, new String[] {"userId"}, true);

		_finderPathCountByUserId = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countByUserId",
			new String[] {Long.class.getName()}, new String[] {"userId"},
			false);

		CalcEntryUtil.setPersistence(this);
	}

	@Deactivate
	public void deactivate() {
		CalcEntryUtil.setPersistence(null);

		entityCache.removeCache(CalcEntryImpl.class.getName());
	}

	@Override
	@Reference(
		target = TestFactoryPersistenceConstants.SERVICE_CONFIGURATION_FILTER,
		unbind = "-"
	)
	public void setConfiguration(Configuration configuration) {
	}

	@Override
	@Reference(
		target = TestFactoryPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Override
	@Reference(
		target = TestFactoryPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	@Reference
	protected EntityCache entityCache;

	@Reference
	protected FinderCache finderCache;

	private static final String _SQL_SELECT_CALCENTRY =
		"SELECT calcEntry FROM CalcEntry calcEntry";

	private static final String _SQL_SELECT_CALCENTRY_WHERE =
		"SELECT calcEntry FROM CalcEntry calcEntry WHERE ";

	private static final String _SQL_COUNT_CALCENTRY =
		"SELECT COUNT(calcEntry) FROM CalcEntry calcEntry";

	private static final String _SQL_COUNT_CALCENTRY_WHERE =
		"SELECT COUNT(calcEntry) FROM CalcEntry calcEntry WHERE ";

	private static final String _ORDER_BY_ENTITY_ALIAS = "calcEntry.";

	private static final String _NO_SUCH_ENTITY_WITH_PRIMARY_KEY =
		"No CalcEntry exists with the primary key ";

	private static final String _NO_SUCH_ENTITY_WITH_KEY =
		"No CalcEntry exists with the key {";

	private static final Log _log = LogFactoryUtil.getLog(
		CalcEntryPersistenceImpl.class);

	@Override
	protected FinderCache getFinderCache() {
		return finderCache;
	}

}