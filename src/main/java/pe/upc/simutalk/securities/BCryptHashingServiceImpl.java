package pe.upc.simutalk.securities;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt implementation. Registered as the {@code passwordEncoder} bean in
 * {@code SecurityConfiguration} so there is exactly one PasswordEncoder.
 */
public class BCryptHashingServiceImpl extends BCryptPasswordEncoder implements HashingService {
}
