package com.reviewbot.auth.oauth2;

import com.reviewbot.auth.domain.User;
import com.reviewbot.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String githubId    = String.valueOf(attributes.get("id"));
        String githubLogin = (String) attributes.get("login");
        String email       = (String) attributes.get("email");
        String avatarUrl   = (String) attributes.get("avatar_url");
        String accessToken = request.getAccessToken().getTokenValue();

        userRepository.findByGithubId(githubId)
                .ifPresentOrElse(
                        u -> u.update(githubLogin, email, avatarUrl, accessToken),
                        () -> userRepository.save(User.of(githubId, githubLogin, email, avatarUrl, accessToken))
                );

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "login"
        );
    }
}
