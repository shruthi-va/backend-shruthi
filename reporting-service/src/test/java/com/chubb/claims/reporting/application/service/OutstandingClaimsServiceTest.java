package com.chubb.claims.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.common.security.CurrentUser;
import com.chubb.claims.reporting.application.port.out.OutstandingClaimsReader;
import com.chubb.claims.reporting.application.query.PageResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OutstandingClaimsServiceTest {

    private static final CurrentUser MANAGER = new CurrentUser("mgr-1", "SG", Set.of(CurrentUser.MANAGER));
    private static final CurrentUser STAFF = new CurrentUser("staff-1", "SG", Set.of(CurrentUser.CLAIMS_STAFF));

    @Mock
    private OutstandingClaimsReader reader;

    private OutstandingClaimsService service;

    @BeforeEach
    void setUp() {
        service = new OutstandingClaimsService(reader);
    }

    @Test
    void staffCannotViewOutstandingClaims() {
        assertThatThrownBy(() -> service.summary(STAFF, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.claims(STAFF, null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        verify(reader, never()).summarize(anySet(), any());
    }

    @Test
    void pageSizeIsClampedAndBlankFiltersBecomeNull() {
        when(reader.list(anySet(), eq(null), eq(null), eq(0), eq(100)))
                .thenReturn(new PageResult<>(List.of(), 0, 100, 0, 0));

        PageResult<?> result = service.claims(MANAGER, "  ", "", -3, 500);

        assertThat(result.size()).isEqualTo(100);
        verify(reader).list(anySet(), eq(null), eq(null), eq(0), eq(100));
    }
}
