package pe.upc.simutalk.securities;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing as seen by the application layer, joined with Spring
 * Security's {@link PasswordEncoder}. The two methods of the former
 * application port (encode, matches) are exactly the PasswordEncoder
 * signatures, so a single bean serves both the sign-up/sign-in flow and
 * Spring Security.
 */
public interface HashingService extends PasswordEncoder {
}
