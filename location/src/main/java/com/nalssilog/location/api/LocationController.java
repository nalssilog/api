package com.nalssilog.location.api;

import com.nalssilog.location.api.dto.LocationResponse;
import com.nalssilog.location.application.LocationService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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
    public List<LocationResponse> search(@RequestParam @NotBlank String keyword) {
        return locationService.search(keyword).stream()
                .map(LocationResponse::from)
                .toList();
    }

    @GetMapping("/reverse-geocode")
    public LocationResponse reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        return LocationResponse.from(locationService.reverseGeocode(lat, lng));
    }

    @GetMapping("/popular")
    public List<LocationResponse> popular() {
        return locationService.getPopular().stream()
                .map(LocationResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public LocationResponse detail(@PathVariable Long id) {
        return LocationResponse.from(locationService.getLocation(id));
    }
}
