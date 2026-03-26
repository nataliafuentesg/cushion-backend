package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No encontrado: " + email));

        // NUEVO: Convertimos el rol de nuestra base de datos al formato que entiende Spring Security
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + client.getRole())
        );

        return User.builder()
                .username(client.getEmail())
                .password(client.getPassword())
                .authorities(authorities) // Ahora sí le pasamos los permisos reales
                .build();
    }
}