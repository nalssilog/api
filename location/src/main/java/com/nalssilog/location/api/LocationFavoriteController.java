package com.nalssilog.location.api;

import com.nalssilog.location.api.dto.FavoriteRequest;
import com.nalssilog.location.api.dto.LocationResponse;
import com.nalssilog.location.application.LocationFavoriteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations/favorites")
@RequiredArgsConstructor
public class LocationFavoriteController {

    private final LocationFavoriteService locationFavoriteService;

    @GetMapping
    public List<LocationResponse> myFavorites(@AuthenticationPrincipal Long memberId) {
        return locationFavoriteService.listFavorites(memberId).stream()
                .map(LocationResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addFavorite(@AuthenticationPrincipal Long memberId, @Valid @RequestBody FavoriteRequest request) {
        locationFavoriteService.addFavorite(memberId, request.locationId());
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@AuthenticationPrincipal Long memberId, @PathVariable Long locationId) {
        locationFavoriteService.removeFavorite(memberId, locationId);
    }
}
