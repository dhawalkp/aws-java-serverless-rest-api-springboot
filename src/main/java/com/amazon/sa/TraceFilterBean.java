package com.amazon.sa;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceFilterBean {
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public FilterRegistrationBean AWSTracingFilterRegistration() {

	    FilterRegistrationBean registration = new FilterRegistrationBean();
	    registration.setFilter(AWSXRayServletFilter());
	    registration.addUrlPatterns("/*");
	    registration.addInitParameter("fixedName", "paramValue");
	    registration.setName("AWSXRayServletFilter");
	    System.out.println("AWS XRay Registered");
	    registration.setOrder(1);
	    return registration;
	} 

	public Filter AWSXRayServletFilter() {
	    return new com.amazonaws.xray.javax.servlet.AWSXRayServletFilter();
	}
	

}
