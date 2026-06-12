package com.shopwavefusion.service;

import com.shopwavefusion.exception.CartItemException;
import com.shopwavefusion.exception.ProductException;
import com.shopwavefusion.exception.UserException;
import com.shopwavefusion.modal.Cart;
import com.shopwavefusion.modal.CartItem;
import com.shopwavefusion.modal.User;
import com.shopwavefusion.request.AddItemRequest;

public interface CartService {
	
	public Cart createCart(User user);
	
	public CartItem addCartItem(Long userId,AddItemRequest req) throws ProductException, CartItemException, UserException;
	
	public Cart findUserCart(Long userId);

}
