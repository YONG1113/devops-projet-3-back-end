package com.openclassrooms.datashare.service;

import com.openclassrooms.datashare.entities.User;
import com.openclassrooms.datashare.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public String login(String login, String password) {
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");

        Optional<User> user = userRepository.findByLogin(login);

        if (!user.isPresent()) {
            throw new UsernameNotFoundException("User not found");
        }

        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.get().getLogin())
                .password(user.get().getPassword())
                .authorities(user.get().getAuthorities())
                .build();
        return jwtService.generateToken(userDetails);
    }


}
