package com.DATN.Bej.service;

import com.DATN.Bej.dto.response.PageResponse;
import com.DATN.Bej.dto.response.productResponse.ProductListResponse;
import com.DATN.Bej.dto.response.productResponse.ProductResponse;
import com.DATN.Bej.entity.product.Product;
import com.DATN.Bej.exception.AppException;
import com.DATN.Bej.exception.ErrorCode;
import com.DATN.Bej.mapper.product.CategoryMapper;
import com.DATN.Bej.mapper.product.ProductAttributeMapper;
import com.DATN.Bej.mapper.product.ProductMapper;
import com.DATN.Bej.mapper.product.ProductVariantMapper;
import com.DATN.Bej.repository.product.CategoryRepository;
import com.DATN.Bej.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductVariantMapper productVariantMapper;

    @Mock
    private ProductAttributeMapper productAttributeMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductService productService;

    private Product buildProduct(String id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setImage("https://img.example/" + id + ".jpg");
        product.setCreateDate(LocalDate.now());
        product.setStatus(1);
        return product;
    }

    private ProductListResponse buildListResponse(String id, String name) {
        ProductListResponse response = new ProductListResponse();
        response.setId(id);
        response.setName(name);
        response.setImage("https://img.example/" + id + ".jpg");
        response.setStatus(1);
        response.setCreateDate(LocalDate.now());
        return response;
    }

    private ProductResponse buildDetailResponse(String id, String name) {
        ProductResponse response = new ProductResponse();
        response.setId(id);
        response.setName(name);
        response.setImage("https://img.example/" + id + ".jpg");
        response.setDescription("detail for " + name);
        response.setStatus(1);
        response.setCreateDate(LocalDate.now());
        return response;
    }

    @Test
    public void getProducts_shouldReturnMappedActiveProductList() {
        Product product1 = buildProduct("p1", "iphone 15");
        Product product2 = buildProduct("p2", "samsung s24");

        ProductListResponse response1 = buildListResponse("p1", "iphone 15");
        ProductListResponse response2 = buildListResponse("p2", "samsung s24");

        when(productRepository.findByStatusOrderByCreateDateDesc(1))
                .thenReturn(List.of(product1, product2));
        when(productMapper.toProductListResponse(product1)).thenReturn(response1);
        when(productMapper.toProductListResponse(product2)).thenReturn(response2);

        List<ProductListResponse> result = productService.getProducts();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("p1", result.get(0).getId());
        assertEquals("p2", result.get(1).getId());

        verify(productRepository).findByStatusOrderByCreateDateDesc(1);
        verify(productMapper).toProductListResponse(product1);
        verify(productMapper).toProductListResponse(product2);
    }

    @Test
    public void getProductsPaginated_shouldNormalizeInvalidPageAndSize() {
        Product product = buildProduct("p1", "iphone 15");
        ProductListResponse responseItem = buildListResponse("p1", "iphone 15");

        Page<Product> pageResult = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createDate")),
                1
        );

        when(productRepository.findByStatusOrderByCreateDateDesc(eq(1), any(Pageable.class)))
                .thenReturn(pageResult);
        when(productMapper.toProductListResponse(product))
                .thenReturn(responseItem);

        PageResponse<ProductListResponse> result = productService.getProductsPaginated(-5, 0);

        assertNotNull(result);
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1, result.getContent().size());
        assertEquals("p1", result.getContent().get(0).getId());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByStatusOrderByCreateDateDesc(eq(1), pageableCaptor.capture());

        Pageable actualPageable = pageableCaptor.getValue();
        assertEquals(0, actualPageable.getPageNumber());
        assertEquals(20, actualPageable.getPageSize());
        assertEquals(Sort.Direction.DESC,
                actualPageable.getSort().getOrderFor("createDate").getDirection());

        verify(productMapper).toProductListResponse(product);
    }

    @Test
    public void getProductsPaginated_shouldCapSizeAt100() {
        Page<Product> pageResult = new PageImpl<>(
                List.of(),
                PageRequest.of(1, 100, Sort.by(Sort.Direction.DESC, "createDate")),
                0
        );

        when(productRepository.findByStatusOrderByCreateDateDesc(eq(1), any(Pageable.class)))
                .thenReturn(pageResult);

        PageResponse<ProductListResponse> result = productService.getProductsPaginated(1, 500);

        assertNotNull(result);
        assertEquals(100, result.getSize());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByStatusOrderByCreateDateDesc(eq(1), pageableCaptor.capture());

        Pageable actualPageable = pageableCaptor.getValue();
        assertEquals(1, actualPageable.getPageNumber());
        assertEquals(100, actualPageable.getPageSize());
        assertEquals(Sort.Direction.DESC,
                actualPageable.getSort().getOrderFor("createDate").getDirection());
    }

    @Test
    public void searchProductsPaginated_shouldSearchByCategoryAndName() {
        Long categoryId = 10L;
        String name = "iphone";

        Product product = buildProduct("p1", "iphone 15");
        ProductListResponse responseItem = buildListResponse("p1", "iphone 15");

        Page<Product> pageResult = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createDate")),
                1
        );

        when(productRepository.findByCategoryAndNameContainingIgnoreCaseAndStatus(
                eq(categoryId), eq(name), eq(1), any(Pageable.class)))
                .thenReturn(pageResult);
        when(productMapper.toProductListResponse(product))
                .thenReturn(responseItem);

        PageResponse<ProductListResponse> result =
                productService.searchProductsPaginated(categoryId, name, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("iphone 15", result.getContent().get(0).getName());
        assertEquals("p1", result.getContent().get(0).getId());

        verify(productRepository).findByCategoryAndNameContainingIgnoreCaseAndStatus(
                eq(categoryId), eq(name), eq(1), any(Pageable.class));
        verify(productRepository, never())
                .findByCategory_IdAndStatusOrderByCreateDateDesc(anyLong(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseAndStatus(anyString(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByStatusOrderByCreateDateDesc(anyInt(), any(Pageable.class));
        verify(productMapper).toProductListResponse(product);
    }

    @Test
    public void searchProductsPaginated_shouldSearchByCategoryOnly() {
        Long categoryId = 2L;

        Product product = buildProduct("p2", "macbook air");
        ProductListResponse responseItem = buildListResponse("p2", "macbook air");

        Page<Product> pageResult = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createDate")),
                1
        );

        when(productRepository.findByCategory_IdAndStatusOrderByCreateDateDesc(
                eq(categoryId), eq(1), any(Pageable.class)))
                .thenReturn(pageResult);
        when(productMapper.toProductListResponse(product)).thenReturn(responseItem);

        PageResponse<ProductListResponse> result =
                productService.searchProductsPaginated(categoryId, "   ", 0, 5);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("macbook air", result.getContent().get(0).getName());

        verify(productRepository).findByCategory_IdAndStatusOrderByCreateDateDesc(
                eq(categoryId), eq(1), any(Pageable.class));
        verify(productRepository, never())
                .findByCategoryAndNameContainingIgnoreCaseAndStatus(anyLong(), anyString(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseAndStatus(anyString(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByStatusOrderByCreateDateDesc(anyInt(), any(Pageable.class));
    }

    @Test
    public void searchProductsPaginated_shouldSearchByNameOnly() {
        String name = "samsung";

        Product product = buildProduct("p3", "samsung s24");
        ProductListResponse responseItem = buildListResponse("p3", "samsung s24");

        Page<Product> pageResult = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createDate")),
                1
        );

        when(productRepository.findByNameContainingIgnoreCaseAndStatus(
                eq(name), eq(1), any(Pageable.class)))
                .thenReturn(pageResult);
        when(productMapper.toProductListResponse(product)).thenReturn(responseItem);

        PageResponse<ProductListResponse> result =
                productService.searchProductsPaginated(null, name, 0, 5);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("samsung s24", result.getContent().get(0).getName());

        verify(productRepository).findByNameContainingIgnoreCaseAndStatus(
                eq(name), eq(1), any(Pageable.class));
        verify(productRepository, never())
                .findByCategoryAndNameContainingIgnoreCaseAndStatus(anyLong(), anyString(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByCategory_IdAndStatusOrderByCreateDateDesc(anyLong(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByStatusOrderByCreateDateDesc(anyInt(), any(Pageable.class));
    }

    @Test
    public void searchProductsPaginated_shouldReturnAllWhenNoCondition() {
        Product product = buildProduct("p4", "ipad pro");
        ProductListResponse responseItem = buildListResponse("p4", "ipad pro");

        Page<Product> pageResult = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createDate")),
                1
        );

        when(productRepository.findByStatusOrderByCreateDateDesc(eq(1), any(Pageable.class)))
                .thenReturn(pageResult);
        when(productMapper.toProductListResponse(product)).thenReturn(responseItem);

        PageResponse<ProductListResponse> result =
                productService.searchProductsPaginated(null, "   ", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("ipad pro", result.getContent().get(0).getName());

        verify(productRepository).findByStatusOrderByCreateDateDesc(eq(1), any(Pageable.class));
        verify(productRepository, never())
                .findByCategoryAndNameContainingIgnoreCaseAndStatus(anyLong(), anyString(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByCategory_IdAndStatusOrderByCreateDateDesc(anyLong(), anyInt(), any(Pageable.class));
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseAndStatus(anyString(), anyInt(), any(Pageable.class));
    }

    @Test
    public void getProductsByCategory_shouldReturnMappedCategoryProducts() {
        Long categoryId = 3L;
        Product product = buildProduct("p5", "mac mini");
        ProductListResponse responseItem = buildListResponse("p5", "mac mini");

        when(productRepository.findByCategory_IdAndStatusOrderByCreateDateDesc(categoryId, 1))
                .thenReturn(List.of(product));
        when(productMapper.toProductListResponse(product)).thenReturn(responseItem);

        List<ProductListResponse> result = productService.getProductsByCategory(categoryId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("mac mini", result.get(0).getName());

        verify(productRepository).findByCategory_IdAndStatusOrderByCreateDateDesc(categoryId, 1);
        verify(productMapper).toProductListResponse(product);
    }

    @Test
    public void searchProductsByName_shouldReturnMappedMatchingProducts() {
        String name = "ipad";
        Product product = buildProduct("p6", "ipad air");
        ProductListResponse responseItem = buildListResponse("p6", "ipad air");

        when(productRepository.findByNameContainingIgnoreCaseAndStatus(name, 1))
                .thenReturn(List.of(product));
        when(productMapper.toProductListResponse(product)).thenReturn(responseItem);

        List<ProductListResponse> result = productService.searchProductsByName(name);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("p6", result.get(0).getId());
        assertEquals("ipad air", result.get(0).getName());

        verify(productRepository).findByNameContainingIgnoreCaseAndStatus(name, 1);
        verify(productMapper).toProductListResponse(product);
    }

    @Test
    public void getProductDetails_shouldReturnMappedProductResponse() {
        String productId = "p7";
        Product product = buildProduct(productId, "apple watch");
        ProductResponse response = buildDetailResponse(productId, "apple watch");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toProductResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductDetails(productId);

        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("apple watch", result.getName());

        verify(productRepository).findById(productId);
        verify(productMapper).toProductResponse(product);
    }

    @Test
    public void getProductDetails_shouldThrowAppExceptionWhenProductNotFound() {
        String productId = "not-found";
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> productService.getProductDetails(productId));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_EXISTED.getMessage(), exception.getMessage());

        verify(productRepository, times(1)).findById(productId);
        verify(productMapper, never()).toProductResponse(any(Product.class));
    }
}
