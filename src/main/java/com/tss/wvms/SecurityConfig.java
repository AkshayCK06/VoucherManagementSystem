package com.tss.wvms;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.tss.wvms.generic.Generic;
import com.tss.wvms.service.VoucherGenerationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@Component
public class SecurityConfig implements HandlerInterceptor {
   
   @Value("${WVMSGUI_API_USER}")
   private String user;
   @Value("${WVMSGUI_API_PASSWORD}")
   private String pass;
   
   @Value("${WVMS_EAPI_UNAME_PWORD}")
   private String apiAuthUserNamePassword;
  
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       System.out.println("authorization starts................");
       String requestURI = request.getRequestURI();
       log.info("`url::::::::`" + requestURI);

       Map<String,String> apiUsernamePasswordHash = new HashMap<String,String>();
       apiUsernamePasswordHash=Generic.stringToHash(apiAuthUserNamePassword,",");
       
       log.info(":::::apiUsernamePasswordHash:::::::::"+apiUsernamePasswordHash);
       
       if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
           // Allow OPTIONS requests for CORS preflight checks
           return true;
       }
       String header = request.getHeader("authorization");
       log.info("`header::`" + header);
       if(header==null) {
    	   log.warn("Authorization header missing for endpoint '{}'.", requestURI);
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      return false;
   }
       log.info(":::::::::::::::::" + StringUtils.hasText(header));
       log.info(":::::::::::::::::::::" + header.toLowerCase().startsWith("basic"));
       if (StringUtils.hasText(header) && header.toLowerCase().startsWith("basic")) {

         String base64Credentials = header.substring("Basic".length()).trim();
         byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
         String credentials = new String(credDecoded, StandardCharsets.UTF_8);
         // credentials = username:password
         final String[] values = credentials.split(":", 2);
         
         try {
        	 if(values.length == 2 && values[1].equals(values[0]) && apiUsernamePasswordHash.get(values[0]).equals(values[1]) && apiUsernamePasswordHash.get(values[1]).equals(values[0]))
             {	 
            	 log.info("User '{}' successfully authenticated for endpoint '{}'.", values[0], requestURI);
                 return true;
             }
             else {
            	 log.warn("Invalid credentials provided by user '{}' for endpoint '{}'.",
                      values.length > 0 ? values[0] : "Unknown", requestURI);
             }
         }
         catch(Exception e)
         {
        	 log.warn("Authorization header missing or invalid for endpoint '{}'.", requestURI);
             response.setStatus(HttpStatus.UNAUTHORIZED.value());
             return false;
         }
       }

       log.warn("Authorization header missing or invalid for endpoint '{}'.", requestURI);
       response.setStatus(HttpStatus.UNAUTHORIZED.value());
       return false;
   }

   @Override
   public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {
   }

   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
   }


}

