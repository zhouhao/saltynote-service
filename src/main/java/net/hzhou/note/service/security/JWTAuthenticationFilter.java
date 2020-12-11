package net.hzhou.note.service.security;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.hzhou.note.service.domain.LoginUser;
import net.hzhou.note.service.entity.RefreshToken;
import net.hzhou.note.service.entity.SiteUser;
import net.hzhou.note.service.repository.RefreshTokenRepository;
import net.hzhou.note.service.component.JwtInstance;

@Slf4j
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository tokenRepository;
  private final JwtInstance jwtInstance;

  public JWTAuthenticationFilter(
      AuthenticationManager authenticationManager, RefreshTokenRepository tokenRepository, JwtInstance jwtInstance) {
    this.authenticationManager = authenticationManager;
    this.tokenRepository = tokenRepository;
    this.jwtInstance = jwtInstance;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
      throws AuthenticationException {
    try {
      SiteUser user = new ObjectMapper().readValue(req.getInputStream(), SiteUser.class);

      return authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              user.getUsername(), user.getPassword(), Collections.emptyList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void successfulAuthentication(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain, Authentication auth)
      throws IOException {

    LoginUser user = (LoginUser) auth.getPrincipal();
    String accessToken = jwtInstance.createAccessToken(user);
    String refreshToken = processRefreshToken(user);
    res.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + accessToken);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    res.getWriter().write(jwtInstance.tokenToJson(accessToken, refreshToken));
  }

  private String processRefreshToken(LoginUser user) {
    String refreshToken = jwtInstance.createRefreshToken(user);
    RefreshToken token = new RefreshToken().setUserId(user.getId()).setRefreshToken(refreshToken);
    tokenRepository.save(token);
    return refreshToken;
  }
}
