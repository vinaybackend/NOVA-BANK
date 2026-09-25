package com.nova.bank.jwtConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt source) {

            List<String> roles = source.getClaimAsStringList("roles");
            System.out.println(roles);

            if (roles == null) {
                return List.of();
            }

            List<GrantedAuthority> collect = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .collect(Collectors.toList());
            System.out.println(collect);
            return collect;
        }
    }
