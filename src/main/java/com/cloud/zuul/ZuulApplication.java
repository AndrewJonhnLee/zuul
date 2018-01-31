package com.cloud.zuul;

import com.cloud.zuul.okhttp.OkHttpRibbonInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


@EnableZuulProxy
@EnableEurekaClient
@SpringBootApplication
public class ZuulApplication {

	@Autowired
	private OkHttpRibbonInterceptor interceptor;

	public static void main(String[] args) {
		SpringApplication.run(ZuulApplication.class, args);
	}

	@Bean
	@LoadBalanced
	@Primary
	public OkHttpClient okHttpClient(){
//		return new OkHttpClient.Builder().addInterceptor(interceptor).build();
		return new OkHttpClient.Builder().build();
	}

	@Bean(name = "loginOkClient")
	@LoadBalanced
	public OkHttpClient loginOkHttpClient(){
		return new OkHttpClient.Builder().addInterceptor(interceptor).build();
	}
//
//	@Bean
//	public WebMvcConfigurer corsConfigurer() {
//		return new WebMvcConfigurerAdapter() {
//			@Override
//			public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/**").allowedOrigins("*")
//						.allowedMethods("*").allowedHeaders("*")
//						.allowCredentials(true)
//						.exposedHeaders("x-application-context ","date","authorization","Authorization").maxAge(3600L);
//			}
//		};
//	}


	@Bean
	public CorsFilter corsFilter() {
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		final CorsConfiguration config = new CorsConfiguration();
		//允许跨域
		config.setAllowCredentials(true);
		//允许向该服务器提交请求的URI,*表示全部
		config.addAllowedOrigin("*");
		//允许访问的头信息,*表示全部
		config.addAllowedHeader("*");
		//允许的method
		config.addAllowedMethod("OPTIONS");
		config.addAllowedMethod("HEAD");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("PUT");
		config.addAllowedMethod("POST");
		config.addAllowedMethod("DELETE");
		config.addAllowedMethod("PATCH");
		config.addExposedHeader("x-application-context");
		config.addExposedHeader("authorization");
		config.addExposedHeader("Authorization");
		config.addExposedHeader("date");
		//免检时间,单位是秒
		//config.setMaxAge(3600)
		//Enabling CORS for the whole application
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}


}
