package ai.gradpath.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ai.gradpath.security.OnboardingInterceptor;

/** MVC configuration: the onboarding gate for students. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OnboardingInterceptor())
                .addPathPatterns("/student/**", "/onboarding", "/messages/**", "/rules")
                .excludePathPatterns("/css/**", "/js/**", "/img/**");
    }
}
