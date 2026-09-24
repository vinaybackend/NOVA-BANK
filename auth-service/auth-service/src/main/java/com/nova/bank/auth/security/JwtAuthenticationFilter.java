package com.nova.bank.auth.security;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private JwtHelper jwtHelper;

   private UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtHelper jwtHelper, UserDetailsService userDetailsService) {
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Bearer kwefwkefwlkfhklf its formate of token
        logger.info("REQUEST URI = {}", request.getRequestURI());
        logger.info("REQUEST METHOD = {}", request.getMethod());
        String authorizationHeader = request.getHeader("Authorization");
        String username=null;
        String token =null;

        logger.trace("request to jwt filter : {}" , authorizationHeader);

        if (authorizationHeader!=null && authorizationHeader.startsWith("Bearer ")){
         try {
             token = authorizationHeader.substring(7);
             username = jwtHelper.getUsernameFromToken(token);
             logger.trace("username of token {}",username);

             if (username!=null && SecurityContextHolder.getContext().getAuthentication()==null){

                 // validate the token
                 UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                 if (jwtHelper.isTokenValid(token,userDetails)){
                     // add data into the spring security

                     UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                     // its help to store extra info about use like ip address and session id
                     authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                     SecurityContextHolder.getContext().setAuthentication(authentication);
                     logger.trace("authentication set in securityContext ");

                     // above three line if execute then ok if not execute then user not get permission

                 }

             }
         }
         catch (IllegalArgumentException e){
             System.out.println("Unable to get JWT Token ");
             logger.error("unable to get JWT token ");
             e.printStackTrace();
         }

         catch (ExpiredJwtException e){
             logger.error("token is expire ");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("\"message\":\"token is expire\",\"success\":\"false\"");
            return;
//             e.printStackTrace();
         }
         catch (MalformedJwtException e){
             logger.error("invalid jwt token ");
             System.out.println("Invalid JWT token ");
             e.printStackTrace();
         }
         catch (Exception e){
             logger.error("invalid token");
             System.out.println("invalid token ");
             e.printStackTrace();
         }

        }else {
            System.out.println("invalid header filter ");
            logger.error("invalid header filter ");
        }

        // its forward the request
        filterChain.doFilter(request,response);


    }
}
