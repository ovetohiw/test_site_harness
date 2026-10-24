package com.perfumeshop.service;

import com.perfumeshop.domain.product.Product;
import com.perfumeshop.dto.catalog.ProductResponse;
import com.perfumeshop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    public enum SortField {
        PRICE, NAME, BRAND;

        static SortField from(String value) {
            if (value == null || value.isBlank()) {
                return NAME;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "price" -> PRICE;
                case "brand" -> BRAND;
                case "name" -> NAME;
                default -> throw new IllegalArgumentException("Unsupported sort: " + value);
            };
        }
    }

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String sort, String direction) {
        SortField field = SortField.from(sort);
        boolean ascending = !"desc".equalsIgnoreCase(direction);

        Comparator<Product> comparator = switch (field) {
            case PRICE -> Comparator.comparing(Product::getPrice);
            case BRAND -> Comparator.comparing(Product::getBrand, String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }

        return productRepository.findAll().stream()
                .sorted(comparator)
                .map(ProductResponse::from)
                .toList();
    }
}
