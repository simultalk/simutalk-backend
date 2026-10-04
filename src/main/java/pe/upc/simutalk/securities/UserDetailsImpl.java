package pe.upc.simutalk.securities;

import pe.upc.simutalk.entities.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security view of a {@link User}. Exposes the user id so method security can
 * compare it (e.g. {@code #userId == authentication.principal.id}).
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    @JsonIgnore
    private final String password;
    private final List<GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String password, List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = List.copyOf(authorities);
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoleNames().stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        return new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword(), authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
