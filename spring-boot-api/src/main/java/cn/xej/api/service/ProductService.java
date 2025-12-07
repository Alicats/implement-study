package cn.xej.api.service;

import cn.xej.api.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {
    
    private final Map<Long, Product> productMap = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();
    
    public ProductService() {
        // 初始化一些示例数据
        productMap.put(counter.incrementAndGet(), new Product(counter.get(), "笔记本电脑", 5999.99));
        productMap.put(counter.incrementAndGet(), new Product(counter.get(), "手机", 3999.99));
    }
    
    /**
     * 获取所有产品
     * @return 产品列表
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(productMap.values());
    }
    
    /**
     * 根据ID获取产品
     * @param id 产品ID
     * @return 产品信息
     */
    public Optional<Product> getProductById(Long id) {
        return Optional.ofNullable(productMap.get(id));
    }
    
    /**
     * 创建新产品
     * @param product 产品信息
     * @return 创建的产品
     */
    public Product createProduct(Product product) {
        Long id = counter.incrementAndGet();
        product.setId(id);
        productMap.put(id, product);
        return product;
    }
    
    /**
     * 更新产品信息
     * @param id 产品ID
     * @param product 更新的产品信息
     * @return 更新后的产品信息
     */
    public Optional<Product> updateProduct(Long id, Product product) {
        if (productMap.containsKey(id)) {
            product.setId(id);
            productMap.put(id, product);
            return Optional.of(product);
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * 删除产品
     * @param id 产品ID
     * @return 是否删除成功
     */
    public boolean deleteProduct(Long id) {
        return productMap.remove(id) != null;
    }
}