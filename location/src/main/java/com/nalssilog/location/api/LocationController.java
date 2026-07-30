package com.nalssilog.location.api;

import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.api.dto.LocationResponse;
import com.nalssilog.location.api.dto.PopularLocationsResponse;
import com.nalssilog.location.application.LocationService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public PageResponse<LocationResponse> search(
            @RequestParam @NotBlank @Size(max = 50) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page
    ) {

        return locationService.search(keyword, page).map(LocationResponse::from);
    }

    @GetMapping("/reverse-geocode")
    public LocationResponse reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng
    ) {

        return LocationResponse.from(locationService.reverseGeocode(lat, lng));
    }

    @GetMapping("/popular")
    public PopularLocationsResponse popular() {

        return PopularLocationsResponse.from(locationService.getPopular());
    }

    @GetMapping("/{id}")
    public LocationResponse detail(@PathVariable Long id) {

        return LocationResponse.from(locationService.getLocation(id));
    }
}
