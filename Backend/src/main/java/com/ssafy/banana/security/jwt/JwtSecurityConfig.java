package com.ssafy.banana.security.jwt;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ssafy.banana.util.RedisUtil;

public class JwtSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
	private TokenProvider tokenProvider;
	private RedisUtil redisUtil;

	public JwtSecurityConfig(TokenProvider tokenProvider, RedisUtil redisUtil) {
		this.tokenProvider = tokenProvider;
		this.redisUtil = redisUtil;
	}

	@Override
	public void configure(HttpSecurity http) {
		http.addFilterBefore(
			new JwtFilter(tokenProvider, redisUtil),
			UsernamePasswordAuthenticationFilter.class
		);
	}
}