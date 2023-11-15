package project.models.enums;

import org.springframework.security.core.GrantedAuthority;


public enum Role implements GrantedAuthority{
    ROLE_BOSS, ROLE_SPECIALIST;

    @Override
    public String getAuthority() {
        return name();
    }
}
