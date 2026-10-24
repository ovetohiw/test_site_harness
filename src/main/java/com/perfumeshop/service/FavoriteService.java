package com.perfumeshop.service;

import com.perfumeshop.domain.favorite.Favorite;
import com.perfumeshop.domain.product.Product;
import com.perfumeshop.domain.user.User;
import com.perfumeshop.dto.catalog.ProductResponse;
import com.perfumeshop.repository.FavoriteRepository;
import com.perfumeshop.repository.ProductRepository;
import com.perfumeshop.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(String login) {
        User user = requireUser(login);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getProduct)
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public List<ProductResponse> add(String login, Long productId) {
        User user = requireUser(login);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            favoriteRepository.save(new Favorite(user, product));
        }
        return list(login);
    }

    @Transactional
    public List<ProductResponse> remove(String login, Long productId) {
        User user = requireUser(login);
        favoriteRepository.deleteByUserIdAndProductId(user.getId(), productId);
        return list(login);
    }

    private User requireUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
