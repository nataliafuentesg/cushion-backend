package com.cushion.cushion_backend.service;
import com.cushion.cushion_backend.dto.ProductDTO;
import com.cushion.cushion_backend.dto.ProductImageDTO;
import com.cushion.cushion_backend.dto.ReviewDTO;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.ProductImage;
import com.cushion.cushion_backend.model.Review;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return convertToDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);

        if (dto.getOccasions() != null) {
            product.setOccasions(new ArrayList<>(dto.getOccasions()));
        }
        if (dto.getImages() != null) {
            dto.getImages().forEach(imgDto -> {
                ProductImage img = new ProductImage();
                BeanUtils.copyProperties(imgDto, img);
                product.addImage(img);
            });
        }
        if (dto.getReviews() != null) {
            dto.getReviews().forEach(revDto -> {
                Review review = new Review();
                BeanUtils.copyProperties(revDto, review);
                product.addReview(review);
            });
        }

        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);

        // ✨ NUEVO: Asegurarnos de enviar las ocasiones al Front
        if (product.getOccasions() != null) {
            dto.setOccasions(new ArrayList<>(product.getOccasions()));
        }

        // Convertir Imágenes a DTOs
        if (product.getImages() != null) {
            List<ProductImageDTO> imageDtos = product.getImages().stream().map(img -> {
                ProductImageDTO imgDto = new ProductImageDTO();
                BeanUtils.copyProperties(img, imgDto);
                return imgDto;
            }).collect(Collectors.toList());
            dto.setImages(imageDtos);
        }

        if (product.getReviews() != null) {
            List<ReviewDTO> reviewDtos = product.getReviews().stream().map(rev -> {
                ReviewDTO revDto = new ReviewDTO();
                BeanUtils.copyProperties(rev, revDto);
                return revDto;
            }).collect(Collectors.toList());
            dto.setReviews(reviewDtos);
        }

        return dto;
    }
    @Transactional
    public ReviewDTO addReview(String slug, ReviewDTO reviewDto) {
        if (reviewDto.getSubtitleVerification() != null && !reviewDto.getSubtitleVerification().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acción no permitida");
        }
        if (reviewDto.getComment() == null || reviewDto.getComment().trim().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario es demasiado corto");
        }

        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        Review review = new Review();
        BeanUtils.copyProperties(reviewDto, review);
        review.setDate(LocalDate.now());

        product.addReview(review);
        productRepository.save(product);

        return convertToReviewDTO(review);
    }

    private ReviewDTO convertToReviewDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        BeanUtils.copyProperties(review, dto);
        if (review.getProduct() != null) {
            dto.setProductName(review.getProduct().getName());
        }
        return dto;
    }
}