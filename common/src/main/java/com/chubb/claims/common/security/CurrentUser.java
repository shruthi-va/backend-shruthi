package com.chubb.claims.common.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * The caller of the current request, read from the Keycloak JWT.
 * The market comes from the {@code market} token claim (attribute name pending, see PRD Q3).
 */
public record CurrentUser(String subject, String market, Set<String> roles) {

    public static final String CLAIMANT = "CLAIMANT";
    public static final String CLAIMS_STAFF = "CLAIMS_STAFF";
    public static final String MANAGER = "MANAGER";

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String MARKET_CLAIM = "market";

    public static CurrentUser from(Authentication authentication) {
        if (!(authentication instanceof KeycloakRoleConverter.KeycloakAuthenticationToken token)) {
            throw new IllegalStateException("Unsupported authentication type: " + authentication);
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentUser(
                token.getJwt().getSubject(),
                token.getJwt().getClaimAsString(MARKET_CLAIM),
                roles);
    }

    public boolean isManager() {
        return roles.contains(MANAGER);
    }

    public boolean isStaff() {
        return roles.contains(CLAIMS_STAFF);
    }

    public boolean isClaimant() {
        return roles.contains(CLAIMANT);
    }
}
