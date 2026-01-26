package com.borsibaar.controller;

import com.borsibaar.dto.BarStationRequestDto;
import com.borsibaar.dto.BarStationResponseDto;
import com.borsibaar.entity.User;
import com.borsibaar.service.BarStationService;
import com.borsibaar.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bar-stations")
@RequiredArgsConstructor
public class BarStationController {

    private final BarStationService barStationService;

    private User currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    @GetMapping
    public ResponseEntity<List<BarStationResponseDto>> getAllStations() {
        User user = currentUser();
        SecurityUtils.requireAdminRole(user);
        
        List<BarStationResponseDto> stations = barStationService.getAllStations(user.getOrganizationId());
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/user")
    public ResponseEntity<List<BarStationResponseDto>> getUserStations() {
        User user = currentUser();
        
        List<BarStationResponseDto> stations = barStationService.getUserStations(user.getId(), user.getOrganizationId());
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarStationResponseDto> getStationById(@PathVariable Long id) {
        User user = currentUser();
        
        BarStationResponseDto station = barStationService.getStationById(user.getOrganizationId(), id);
        return ResponseEntity.ok(station);
    }

    @PostMapping
    public ResponseEntity<BarStationResponseDto> createStation(@Valid @RequestBody BarStationRequestDto request) {
        User user = currentUser();
        SecurityUtils.requireAdminRole(user);
        
        BarStationResponseDto station = barStationService.createStation(user.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(station);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarStationResponseDto> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody BarStationRequestDto request) {
        User user = currentUser();
        SecurityUtils.requireAdminRole(user);
        
        BarStationResponseDto station = barStationService.updateStation(user.getOrganizationId(), id, request);
        return ResponseEntity.ok(station);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        User user = currentUser();
        SecurityUtils.requireAdminRole(user);
        
        barStationService.deleteStation(user.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }
}

