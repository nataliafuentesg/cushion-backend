package com.cushion.cushion_backend.service;
import com.cushion.cushion_backend.dto.ProductDTO;
import com.cushion.cushion_backend.dto.ProductImageDTO;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.ProductImage;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (dto.getImages() != null) {
            for (ProductImageDTO imgDto : dto.getImages()) {
                ProductImage img = new ProductImage();
                BeanUtils.copyProperties(imgDto, img);
                product.addImage(img);
            }
        }

        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);

        List<ProductImageDTO> imageDtos = product.getImages().stream().map(img -> {
            ProductImageDTO imgDto = new ProductImageDTO();
            BeanUtils.copyProperties(img, imgDto);
            return imgDto;
        }).collect(Collectors.toList());

        dto.setImages(imageDtos);
        return dto;
    }
}