package com.nalssilog.location.application;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.domain.LocationFavorite;
import com.nalssilog.location.repository.LocationFavoriteRepository;
import com.nalssilog.location.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 즐겨찾기 지역 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationFavoriteService {

	private static final int PAGE_SIZE = 5;
	private static final String FAVORITE_CONSTRAINT = "uk_location_favorite_member_location";

	private final LocationFavoriteRepository locationFavoriteRepository;
	private final LocationRepository locationRepository;

	private static boolean hasConstraint(
		Throwable throwable
	) {
		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				String constraintName = constraintViolation.getConstraintName();

				return FAVORITE_CONSTRAINT.equalsIgnoreCase(constraintName);
			}
		}

		return false;
	}

	@Transactional
	public void addFavorite(Long memberId, Long locationId) {
		locationRepository.getById(locationId);

		if (locationFavoriteRepository.existsByMemberIdAndLocationId(memberId, locationId)) {
			return;
		}

		try {
			locationFavoriteRepository.saveAndFlush(
				LocationFavorite.of(memberId, locationId));
		} catch (DataIntegrityViolationException exception) {
			if (hasConstraint(exception)) {
				throw new NalssiLogException(
					LocationErrorCode.FAVORITE_ALREADY_EXISTS);
			}

			throw exception;
		}
	}

	@Transactional
	public void removeFavorite(Long memberId, Long locationId) {
		locationFavoriteRepository.deleteByMemberIdAndLocationId(memberId, locationId);
	}

	public PageResponse<LocationInfo> listFavorites(Long memberId, int page) {
		Page<LocationFavorite> favorites =
			locationFavoriteRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(
				memberId,
				PageRequest.of(page, PAGE_SIZE));
		List<Long> favoriteIds = favorites.getContent().stream()
			.map(LocationFavorite::getLocationId)
			.toList();

		return PageResponse.of(
			locationRepository.findByIds(favoriteIds),
			page,
			PAGE_SIZE,
			favorites.getTotalElements());
	}
}
