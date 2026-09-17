package com.shopwavefusion.rework.api.v1;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.api.v1.AuthDtos.ProfileUpdateRequest;
import com.shopwavefusion.rework.api.v1.CommonDtos.AddressResponse;
import com.shopwavefusion.rework.api.v1.CommonDtos.UserResponse;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.AddressRepository;
import com.shopwavefusion.rework.service.AuthService;
import com.shopwavefusion.rework.service.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {
    private final CurrentUserService current;
    private final AuthService auth;
    private final AddressRepository addresses;

    public ProfileController(CurrentUserService current, AuthService auth, AddressRepository addresses) {
        this.current = current;
        this.auth = auth;
        this.addresses = addresses;
    }

    @GetMapping
    public UserResponse get(Authentication authentication) { return auth.user(current.require(authentication)); }

    @PatchMapping
    @Transactional
    public UserResponse update(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        UserEntity user = current.require(authentication);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setMobile(request.mobile().trim());
        return auth.user(user);
    }

    @GetMapping("/addresses")
    public List<AddressResponse> addresses(Authentication authentication) {
        UserEntity user = current.require(authentication);
        return addresses.findTop20ByUserIdOrderByIdDesc(user.getId()).stream()
                .map(a -> new AddressResponse(a.getId(), a.getFirstName(), a.getLastName(), a.getStreetAddress(),
                        a.getCity(), a.getDepartment(), a.getPostalCode(), a.getMobile(), a.getCountry())).toList();
    }
}
