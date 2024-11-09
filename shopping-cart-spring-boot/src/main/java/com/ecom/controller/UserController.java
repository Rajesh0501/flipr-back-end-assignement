package com.ecom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.ecom.model.Cart;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@RestController
@RequestMapping("/api/user")
public class UserController {

	@Autowired
	private UserService userService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private CartService cartService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private CommonUtil commonUtil;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/home")
	public ResponseEntity<String> home() {
		return ResponseEntity.ok("Welcome to the user home page.");
	}

	private UserDtls getLoggedInUserDetails(Principal principal) {
		String email = principal.getName();
		return userService.getUserByEmail(email);
	}

	@PostMapping("/addCart")
	public ResponseEntity<String> addToCart(@RequestParam Integer productId, @RequestParam Integer userId) {
		Cart savedCart = cartService.saveCart(productId, userId);
		if (savedCart != null) {
			return ResponseEntity.ok("Product added to cart successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to add product to cart.");
		}
	}

	@GetMapping("/cart")
	public ResponseEntity<List<Cart>> loadCartPage(Principal principal) {
		UserDtls user = getLoggedInUserDetails(principal);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		return ResponseEntity.ok(carts);
	}

	@PutMapping("/cartQuantityUpdate")
	public ResponseEntity<String> updateCartQuantity(@RequestParam String updateType, @RequestParam Integer cartId) {
		cartService.updateQuantity(updateType, cartId);
		return ResponseEntity.ok("Cart quantity updated successfully.");
	}

	@GetMapping("/orders")
	public ResponseEntity<Object> orderPage(Principal principal) {
		UserDtls user = getLoggedInUserDetails(principal);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		if (!carts.isEmpty()) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = orderPrice + 250 + 100;
			return ResponseEntity.ok(new OrderSummary(orderPrice, totalOrderPrice));
		}
		return ResponseEntity.ok("No items in cart.");
	}

	@PostMapping("/save-order")
	public ResponseEntity<String> saveOrder(@RequestBody OrderRequest request, Principal principal) throws Exception {
		UserDtls user = getLoggedInUserDetails(principal);
		orderService.saveOrder(user.getId(), request);
		return ResponseEntity.ok("Order placed successfully.");
	}

	@GetMapping("/user-orders")
	public ResponseEntity<List<ProductOrder>> myOrders(Principal principal) {
		UserDtls user = getLoggedInUserDetails(principal);
		List<ProductOrder> orders = orderService.getOrdersByUser(user.getId());
		return ResponseEntity.ok(orders);
	}

//	@PutMapping("/update-status")
//	public ResponseEntity<String> updateOrderStatus(@RequestParam Integer orderId, @RequestParam Integer statusId) {
//		OrderStatus status = OrderStatus.getById(statusId);
//		ProductOrder updatedOrder = orderService.updateOrderStatus(orderId, status.getName());
//
//		if (updatedOrder != null) {
//			try {
//				commonUtil.sendMailForProductOrder(updatedOrder, status.getName());
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			return ResponseEntity.ok("Order status updated successfully.");
//		} else {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order status update failed.");
//		}
//	}

//	@GetMapping("/profile")
//	public ResponseEntity<UserDtls> profile(Principal principal) {
//		return ResponseEntity.ok(getLoggedInUserDetails(principal));
//	}
//
//	@PutMapping("/update-profile")
//	public ResponseEntity<String> updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img) {
//		UserDtls updatedUser = userService.updateUserProfile(user, img);
//		if (updatedUser != null) {
//			return ResponseEntity.ok("Profile updated successfully.");
//		} else {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile update failed.");
//		}
//	}

	@PutMapping("/change-password")
	public ResponseEntity<String> changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal principal) {
		UserDtls user = getLoggedInUserDetails(principal);
		boolean isMatch = passwordEncoder.matches(currentPassword, user.getPassword());

		if (isMatch) {
			user.setPassword(passwordEncoder.encode(newPassword));
			userService.updateUser(user);
			return ResponseEntity.ok("Password updated successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect current password.");
		}
	}

	private static class OrderSummary {
		private Double orderPrice;
		private Double totalOrderPrice;

		public OrderSummary(Double orderPrice, Double totalOrderPrice) {
			this.orderPrice = orderPrice;
			this.totalOrderPrice = totalOrderPrice;
		}

		public Double getOrderPrice() {
			return orderPrice;
		}

		public void setOrderPrice(Double orderPrice) {
			this.orderPrice = orderPrice;
		}

		public Double getTotalOrderPrice() {
			return totalOrderPrice;
		}

		public void setTotalOrderPrice(Double totalOrderPrice) {
			this.totalOrderPrice = totalOrderPrice;
		}
	}
}
