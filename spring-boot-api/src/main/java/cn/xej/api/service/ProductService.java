package cn.xej.api.service;

import cn.xej.api.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    private List<Product> products = new ArrayList<>();
    
    public ProductService() {
        // 初始化一些产品数据
        products.add(new Product(1L, "笔记本电脑", 5999.99));
        products.add(new Product(2L, "手机", 3999.99));
        products.add(new Product(3L, "平板电脑", 2999.99));
    }
    
    public List<Product> getAllProducts() {
        return products;
    }
    
    public Optional<Product> getProductById(Long id) {
        return products.stream().filter(product -> product.getId().equals(id)).findFirst();
    }
    
    public Product createProduct(Product product) {
        // 设置ID（简单起见，实际项目中应该使用更复杂的ID生成策略）
        product.setId((long) (products.size() + 1));
        products.add(product);
        return product;
    }
    
    public Optional<Product> updateProduct(Long id, Product updatedProduct) {
        Optional<Product> existingProduct = getProductById(id);
        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            product.setName(updatedProduct.getName());
            product.setPrice(updatedProduct.getPrice());
            return Optional.of(product);
        }
        return Optional.empty();
    }
    
    public boolean deleteProduct(Long id) {
        return products.removeIf(product -> product.getId().equals(id));
    }
}
