package com.perfumeshop.web;

import com.perfumeshop.dto.catalog.FavoriteRequest;
import com.perfumeshop.dto.catalog.ProductResponse;
import com.perfumeshop.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<ProductResponse> list(Authentication authentication) {
        return favoriteService.list(authentication.getName());
    }

    @PostMapping
    public List<ProductResponse> add(
            Authentication authentication,
            @Valid @RequestBody FavoriteRequest request
    ) {
        return favoriteService.add(authentication.getName(), request.productId());
    }

    @DeleteMapping("/{productId}")
    public List<ProductResponse> remove(Authentication authentication, @PathVariable Long productId) {
        return favoriteService.remove(authentication.getName(), productId);
    }
}
