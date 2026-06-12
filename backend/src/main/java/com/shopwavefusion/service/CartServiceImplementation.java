package com.shopwavefusion.service;

import org.springframework.stereotype.Service;

import com.shopwavefusion.exception.CartItemException;
import com.shopwavefusion.exception.ProductException;
import com.shopwavefusion.exception.UserException;
import com.shopwavefusion.modal.Cart;
import com.shopwavefusion.modal.CartItem;
import com.shopwavefusion.modal.Product;
import com.shopwavefusion.modal.User;
import com.shopwavefusion.repository.CartRepository;
import com.shopwavefusion.request.AddItemRequest;

@Service
public class CartServiceImplementation implements CartService{
	
	private CartRepository cartRepository;
	private CartItemService cartItemService;
	private ProductService productService;
	
	
	public CartServiceImplementation(CartRepository cartRepository,CartItemService cartItemService,
			ProductService productService) {
		this.cartRepository=cartRepository;
		this.productService=productService;
		this.cartItemService=cartItemService;
	}

	@Override
	public Cart createCart(User user) {
		
		Cart cart = new Cart();
		cart.setUser(user);
		Cart createdCart=cartRepository.save(cart);
		return createdCart;
	}
	
	public Cart findUserCart(Long userId) {
		Cart cart =	cartRepository.findByUserId(userId);

		if(cart==null) {
			return null;
		}

		int totalPrice=0;
		int totalDiscountedPrice=0;
		int totalItem=0;
		for(CartItem cartsItem : cart.getCartItems()) {
			totalPrice+=cartsItem.getPrice();
			totalDiscountedPrice+=cartsItem.getDiscountedPrice();
			totalItem+=cartsItem.getQuantity();
		}

		cart.setTotalPrice(totalPrice);
		cart.setTotalItem(cart.getCartItems().size());
		cart.setTotalDiscountedPrice(totalDiscountedPrice);
		cart.setDiscounte(totalPrice-totalDiscountedPrice);
		cart.setTotalItem(totalItem);

		return cartRepository.save(cart);

	}

	@Override
	public CartItem addCartItem(Long userId, AddItemRequest req) throws ProductException, CartItemException, UserException {
		Cart cart=cartRepository.findByUserId(userId);
		Product product=productService.findProductById(req.getProductId());

		if (product.getQuantity() <= 0) {
			throw new ProductException("Producto sin stock: " + product.getTitle());
		}

		CartItem isPresent=cartItemService.isCartItemExist(cart, product, req.getSize(),userId);
		CartItem createdCartItem=null;
		if(isPresent == null) {
			int qty = Math.min(req.getQuantity(), Math.min(10, product.getQuantity()));
			CartItem cartItem = new CartItem();
			cartItem.setProduct(product);
			cartItem.setCart(cart);
			cartItem.setQuantity(qty);
			cartItem.setUserId(userId);
			
			int price=qty*product.getDiscountedPrice();
			cartItem.setPrice(price);
			cartItem.setSize(req.getSize());
			
			 createdCartItem=cartItemService.createCartItem(cartItem);
			cart.getCartItems().add(createdCartItem);
		} else {
			int newQty = isPresent.getQuantity() + req.getQuantity();
			newQty = Math.min(newQty, Math.min(10, product.getQuantity()));
			isPresent.setQuantity(newQty);
			isPresent.setPrice(newQty * product.getDiscountedPrice());
			isPresent.setDiscountedPrice(newQty * product.getDiscountedPrice());
			cartItemService.updateCartItem(userId, isPresent.getId(), isPresent);
			createdCartItem = isPresent;
		}
		
		return createdCartItem;
	}

}
