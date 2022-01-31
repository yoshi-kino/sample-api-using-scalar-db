package com.example.api.service;

import com.example.api.exception.ObjectNotFoundException;
import com.example.api.model.User;
import com.example.api.model.UserGroup;
import com.example.api.repository.UserRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService
    implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
  private final UserRepository userRepository;
  private final DistributedTransactionManager manager;

  @Autowired
  public AuthenticationService(
      UserRepository userRepository, DistributedTransactionManager manager) {
    this.userRepository = userRepository;
    this.manager = manager;
  }

  @Override
  public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token)
      throws UsernameNotFoundException {
    try {
      String userId = token.getPrincipal().toString();
      List<GrantedAuthority> authorities =
          new ArrayList<GrantedAuthority>(
              Arrays.asList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      if (userId.isEmpty()) {
        return new AccountUser("user", "password", authorities, null, null);
      }

      DistributedTransaction tx = manager.start();
      User user = userRepository.getUser(tx, userId);
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

      List<String> groupNameList = new ArrayList<String>();
      List<String> groupIdList = new ArrayList<String>();
      List<UserGroup> userGroups =
          Optional.ofNullable(user.getUserGroups()).orElse(new ArrayList<UserGroup>());
      userGroups.forEach(
          (userGroup -> {
            groupNameList.add(userGroup.getGroupName());
            groupIdList.add(userGroup.getGroupId());
          }));
      if (groupNameList.contains("admin")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }

      return new AccountUser("user", "password", authorities, user.getUserId(), groupIdList);
    } catch (TransactionException | ObjectNotFoundException e) {
      throw new UsernameNotFoundException("Invalid authorization header.");
    }
  }

  public static class AccountUser extends org.springframework.security.core.userdetails.User {
    String userId;
    List<String> groupIdList;

    public AccountUser(
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        String userId,
        List<String> groupIdList) {
      super(username, password, authorities);
      this.userId = userId;
      this.groupIdList = groupIdList;
    }

    public List<String> getGroupIdList() {
      return groupIdList;
    }

    public String getUserId() {
      return userId;
    }
  }
}
