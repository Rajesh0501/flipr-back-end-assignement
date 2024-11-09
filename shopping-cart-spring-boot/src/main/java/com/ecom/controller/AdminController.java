package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/user-details")
	public ResponseEntity<UserDtls> getUserDetails(Principal principal) {
		if (principal != null) {
			String email = principal.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			return ResponseEntity.ok(userDtls);
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getCategories(
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		Page<Category> page = categoryService.getAllCategorPagination(pageNo, pageSize);
		return ResponseEntity.ok(page.getContent());
	}

	@PostMapping("/category")
	public ResponseEntity<String> saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file) throws IOException {
		if (categoryService.existCategory(category.getName())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Category Name already exists");
		}

		String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imageName);
		Category savedCategory = categoryService.saveCategory(category);

		if (ObjectUtils.isEmpty(savedCategory)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Category not saved due to server error");
		}

		File saveFile = new ClassPathResource("static/img").getFile();
		Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator + imageName);
		Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

		return ResponseEntity.ok("Category saved successfully");
	}

	@DeleteMapping("/category/{id}")
	public ResponseEntity<String> deleteCategory(@PathVariable int id) {
		if (categoryService.deleteCategory(id)) {
			return ResponseEntity.ok("Category deleted successfully");
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting category");
	}

	@PutMapping("/category")
	public ResponseEntity<String> updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file) throws IOException {
		Category existingCategory = categoryService.getCategoryById(category.getId());
		if (existingCategory == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found");
		}

		String imageName = file.isEmpty() ? existingCategory.getImageName() : file.getOriginalFilename();
		existingCategory.setName(category.getName());
		existingCategory.setIsActive(category.getIsActive());
		existingCategory.setImageName(imageName);

		Category updatedCategory = categoryService.saveCategory(existingCategory);

		if (!file.isEmpty()) {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator + imageName);
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		return updatedCategory != null ? ResponseEntity.ok("Category updated successfully") :
				ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating category");
	}

	@PostMapping("/product")
	public ResponseEntity<String> saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image) throws IOException {
		String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());

		Product savedProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(savedProduct)) {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator + imageName);
			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			return ResponseEntity.ok("Product saved successfully");
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving product");
	}

	@GetMapping("/products")
	public ResponseEntity<Page<Product>> getProducts(
			@RequestParam(defaultValue = "") String search,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		Page<Product> page = (search != null && search.length() > 0) ?
				productService.searchProductPagination(pageNo, pageSize, search) :
				productService.getAllProductsPagination(pageNo, pageSize);

		return ResponseEntity.ok(page);
	}

	@DeleteMapping("/product/{id}")
	public ResponseEntity<String> deleteProduct(@PathVariable int id) {
		if (productService.deleteProduct(id)) {
			return ResponseEntity.ok("Product deleted successfully");
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting product");
	}

	@PutMapping("/product")
	public ResponseEntity<String> updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image) {
		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid discount value");
		}

		Product updatedProduct = productService.updateProduct(product, image);

		if (!ObjectUtils.isEmpty(updatedProduct)) {
			return ResponseEntity.ok("Product updated successfully");
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating product");
	}
}
