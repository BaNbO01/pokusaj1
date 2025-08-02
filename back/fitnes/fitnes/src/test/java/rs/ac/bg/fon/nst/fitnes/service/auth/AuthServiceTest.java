
package rs.ac.bg.fon.nst.fitnes.service.auth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.ac.bg.fon.nst.fitnes.domain.Role;
import rs.ac.bg.fon.nst.fitnes.domain.User;
import rs.ac.bg.fon.nst.fitnes.dto.LoginRequest;
import rs.ac.bg.fon.nst.fitnes.dto.RegisterRequest;
import rs.ac.bg.fon.nst.fitnes.dto.UserResponse;
import rs.ac.bg.fon.nst.fitnes.exception.DuplicateEntryException;
import rs.ac.bg.fon.nst.fitnes.exception.ResourceNotFoundException;
import rs.ac.bg.fon.nst.fitnes.mapper.UserMapper;
import rs.ac.bg.fon.nst.fitnes.repo.UserRepository;
import rs.ac.bg.fon.nst.fitnes.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import rs.ac.bg.fon.nst.fitnes.service.auth.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Priprema test objekata za upotrebu u testovima
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setCreatedAt(LocalDateTime.now());

        testRole = new Role("TRENER");
        testRole.setId(1);
        testRole.setUser(testUser);
        testUser.setRole(testRole);

        registerRequest = new RegisterRequest("test_user", "test@example.com", "password123", "trener");
        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void testRegisterUser_Success() {
        // Mock-ovanje ponašanja zavisnosti
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty()); // Provera da korisnik ne postoji
        when(userMapper.toUser(any(RegisterRequest.class))).thenReturn(testUser); // Mapiranje DTO-a u domen objekat
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password"); // Enkodiranje lozinke
        when(userRepository.save(any(User.class))).thenReturn(testUser); // Čuvanje korisnika
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse(1, "test@example.com", null, null)); // Mapiranje u Response DTO

        // Pozivanje metode servisa
        UserResponse result = authService.registerUser(registerRequest);

        // Provera rezultata
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());

        // Provera da li su sve potrebne metode pozvane
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toUser(any(RegisterRequest.class));
        verify(userMapper, times(1)).toUserResponse(any(User.class));
        verify(passwordEncoder, times(1)).encode(anyString());
    }

    @Test
    void testRegisterUser_DuplicateEmail_ThrowsException() {
        // Mock-ovanje da korisnik sa istim email-om već postoji
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Provera da li se baca izuzetak
        assertThrows(DuplicateEntryException.class, () -> authService.registerUser(registerRequest));

        // Provera da save metoda nije pozvana
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginUser_Success() {
        // Mock-ovanje Authentication objekta i JWT providera
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("mock_jwt_token");

        // Pozivanje metode servisa
        String token = authService.loginUser(loginRequest);

        // Provera rezultata
        assertNotNull(token);
        assertEquals("mock_jwt_token", token);

        // Provera poziva metoda
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateToken(authentication);
        // Provera da li je SecurityContextHolder postavljen
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testLogoutUser_Success() {
        // Pozivanje metode za odjavu
        authService.logoutUser();

        // Provera da je SecurityContextHolder očišćen
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testGetUserByEmail_Success() {
        // Mock-ovanje da korisnik postoji
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse(1, "test@example.com", null, null));

        // Pozivanje metode servisa
        UserResponse result = authService.getUserByEmail("test@example.com");

        // Provera rezultata
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());

        // Provera poziva metoda
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userMapper, times(1)).toUserResponse(any(User.class));
    }

    @Test
    void testGetUserByEmail_NotFound_ThrowsException() {
        // Mock-ovanje da korisnik ne postoji
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Provera da li se baca izuzetak
        assertThrows(ResourceNotFoundException.class, () -> authService.getUserByEmail("nepostojeci@email.com"));

        // Provera da li maper nije pozvan
        verify(userMapper, never()).toUserResponse(any(User.class));
    }
}