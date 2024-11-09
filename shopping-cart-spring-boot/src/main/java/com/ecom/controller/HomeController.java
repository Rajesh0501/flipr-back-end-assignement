package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class HomeController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private CartService cartService;

	@GetMapping("/user-details")
	public ResponseEntity<?> getUserDetails(Principal p) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			return ResponseEntity.ok(new UserResponse(userDtls, countCart));
		}
		return ResponseEntity.badRequest().body("User not found");
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getAllCategories() {
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		return ResponseEntity.ok(allActiveCategory);
	}

	@GetMapping("/products")
	public ResponseEntity<Page<Product>> getProducts(
			@RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
			@RequestParam(defaultValue = "") String ch) {

		Page<Product> page;
		if (ch.isEmpty()) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
		}
		return ResponseEntity.ok(page);
	}

	@GetMapping("/product/{id}")
	public ResponseEntity<Product> getProductById(@PathVariable int id) {
		Product productById = productService.getProductById(id);
		return ResponseEntity.ok(productById);
	}

	@PostMapping("/signup")
	public ResponseEntity<?> saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file)
			throws IOException {

		if (userService.existsEmail(user.getEmail())) {
			return ResponseEntity.badRequest().body("Email already exists");
		}

		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls savedUser = userService.saveUser(user);

		if (savedUser != null && !file.isEmpty()) {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
					+ file.getOriginalFilename());
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		}
		return ResponseEntity.ok("User registered successfully");
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<?> processForgotPassword(@RequestParam String email, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {

		UserDtls userByEmail = userService.getUserByEmail(email);
		if (ObjectUtils.isEmpty(userByEmail)) {
			return ResponseEntity.badRequest().body("Invalid email");
		}

		String resetToken = UUID.randomUUID().toString();
		userService.updateUserResetToken(email, resetToken);
		String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;
		Boolean sendMail = commonUtil.sendMail(url, email);

		if (!sendMail) {
			return ResponseEntity.status(500).body("Server error, email not sent");
		}
		return ResponseEntity.ok("Password reset link sent to email");
	}

	@GetMapping("/reset-password")
	public ResponseEntity<?> showResetPassword(@RequestParam String token) {
		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			return ResponseEntity.badRequest().body("Invalid or expired link");
		}
		return ResponseEntity.ok(token);
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String password) {
		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			return ResponseEntity.badRequest().body("Invalid or expired link");
		}

		userByToken.setPassword(passwordEncoder.encode(password));
		userByToken.setResetToken(null);
		userService.updateUser(userByToken);
		return ResponseEntity.ok("Password changed successfully");
	}

	@GetMapping("/search")
	public ResponseEntity<List<Product>> searchProduct(@RequestParam String ch) {
		List<Product> searchProducts = productService.searchProduct(ch);
		return ResponseEntity.ok(searchProducts);
	}

	static class UserResponse {
		private UserDtls user;
		private Integer countCart;

		public UserResponse(UserDtls user, Integer countCart) {
			this.user = user;
			this.countCart = countCart;
		}

		public UserDtls getUser() {
			return user;
		}

		public void setUser(UserDtls user) {
			this.user = user;
		}

		public Integer getCountCart() {
			return countCart;
		}

		public void setCountCart(Integer countCart) {
			this.countCart = countCart;
		}
	}
}
