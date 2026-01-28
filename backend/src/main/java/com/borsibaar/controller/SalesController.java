package com.borsibaar.controller;

import com.borsibaar.dto.SaleRequestDto;
import com.borsibaar.dto.SaleResponseDto;
import com.borsibaar.entity.User;
import com.borsibaar.service.SalesService;
import com.borsibaar.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponseDto processSale(@RequestBody @Valid SaleRequestDto request) {
        User user = SecurityUtils.getCurrentUser();
        return salesService.processSale(request, user.getId(), user.getOrganizationId());
    }
}