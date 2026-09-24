package com.nova.bank.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtHelper {


    private static final long ACCESS_TOKEN_VALIDITY =5*60*1000;

    private static final long REFRESH_TOKEN_VALIDITY=30*60*1000;

    @Value("${jwt.secret}")
    private String SECRET_KEY;
 //   private Key key; we can also use
    private SecretKey key;

    @PostConstruct
    public void init(){
        this.key= Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
    // generate token
    public String generateAccessToken(UserDetails userDetails){

        Map<String,Object> claims= new HashMap<>();
        claims.put("token_type","access_token");
        claims.put("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
      return buildToken(claims,userDetails.getUsername(),ACCESS_TOKEN_VALIDITY);
    }


    public String generateRefreshToken(UserDetails userDetails){

        Map<String,Object> claims=new HashMap<>();
        claims.put("token_type","refresh_token");
        claims.put("roles",userDetails.getAuthorities());
        return buildToken(claims,userDetails.getUsername(),REFRESH_TOKEN_VALIDITY);
    }
    // check it is refresh token
    public boolean isRefreshToken(String token){
        return getTokenType(token).equals("refresh_token");

    }

    public boolean isAccessToken(String token){
        return getTokenType(token).equals("access_token");
    }

    public String getTokenType(String token){
        Object tokenType = getClaims(token).get("token_type");
        return tokenType!=null?tokenType.toString():"";
    }

    //token build method

    private String buildToken(Map<String ,Object> claims,String username,long validity){
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+ validity))
                .signWith(key,Jwts.SIG.HS256)
                .compact();
    }

    // get userName from token

    public String getUsernameFromToken(String token){
        String username = getClaims(token).getSubject();
      return username;
    }

    //validate token

    public boolean isTokenValid(String token,UserDetails userDetails){

        String usernameFromToken = getUsernameFromToken(token);

        return usernameFromToken.equals(userDetails.getUsername()) && !isTokenExpire(token);

    }

    // check token expire or not

    public boolean isTokenExpire(String token){
        boolean before = getClaims(token).getExpiration().before(new Date());
        return before;
    }

    //claims

    public Claims getClaims(String token){
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }



}
