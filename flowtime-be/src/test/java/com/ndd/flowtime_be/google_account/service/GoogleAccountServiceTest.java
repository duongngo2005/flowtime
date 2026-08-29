package com.ndd.flowtime_be.google_account.service;

import com.ndd.flowtime_be.google_account.entity.GoogleAccount;
import com.ndd.flowtime_be.google_account.repository.GoogleAccountRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceTest {

    @Mock
    private GoogleAccountRepository googleAccountRepository;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private GoogleAccountService googleAccountService;

    @Test
    void disconnectDeletesLinkedGoogleAccount() {
        User user = User.builder().email("user@example.com").name("Test User").build();
        GoogleAccount account = GoogleAccount.builder().user(user).build();
        when(googleAccountRepository.findByUser(user)).thenReturn(Optional.of(account));

        googleAccountService.disconnect(user);

        verify(googleAccountRepository).delete(account);
    }
}
