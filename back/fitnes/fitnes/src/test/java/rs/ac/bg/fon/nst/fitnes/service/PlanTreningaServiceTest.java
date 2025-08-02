package rs.ac.bg.fon.nst.fitnes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.ac.bg.fon.nst.fitnes.domain.PlanTreninga;
import rs.ac.bg.fon.nst.fitnes.domain.PlanVezbe;
import rs.ac.bg.fon.nst.fitnes.domain.User;
import rs.ac.bg.fon.nst.fitnes.domain.Vezba;
import rs.ac.bg.fon.nst.fitnes.dto.PlanTreningaRequest;
import rs.ac.bg.fon.nst.fitnes.dto.PlanTreningaResponse;
import rs.ac.bg.fon.nst.fitnes.dto.PlanVezbeRequestItem;
import rs.ac.bg.fon.nst.fitnes.dto.PlanVezbeResponse;
import rs.ac.bg.fon.nst.fitnes.dto.VezbaResponse;
import rs.ac.bg.fon.nst.fitnes.exception.ResourceNotFoundException;
import rs.ac.bg.fon.nst.fitnes.exception.UnauthorizedAccessException;
import rs.ac.bg.fon.nst.fitnes.mapper.PlanTreningaMapper;
import rs.ac.bg.fon.nst.fitnes.mapper.PlanVezbeMapper;
import rs.ac.bg.fon.nst.fitnes.repo.PlanTreningaRepository;
import rs.ac.bg.fon.nst.fitnes.repo.UserRepository;
import rs.ac.bg.fon.nst.fitnes.repo.VezbaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Sort;

/**
 * Test klasa za PlanTreningaService.
 * Koristi JUnit 5 i Mockito za simulaciju zavisnosti.
 */
@ExtendWith(MockitoExtension.class)
class PlanTreningaServiceTest {

    @InjectMocks
    private PlanTreningaService planTreningaService;

    // Mock-ovanje zavisnosti servisa
    @Mock
    private PlanTreningaRepository planTreningaRepository;
    @Mock
    private VezbaRepository vezbaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlanTreningaMapper planTreningaMapper;
    @Mock
    private PlanVezbeMapper planVezbeMapper;

    // Mock-ovanje Spring Security konteksta
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private User vezbac;
    private Vezba vezba;
    private PlanTreninga planTreninga;
    private PlanTreningaRequest planTreningaRequest;
    private PlanTreningaResponse planTreningaResponse;
    private PlanVezbeRequestItem planVezbeRequestItem;
    private PlanVezbe planVezbe;
    private PlanVezbeResponse planVezbeResponse;

    @BeforeEach
    void setUp() {
        // Kreiranje mock objekata za testiranje
        vezbac = new User();
        vezbac.setId(1);
        vezbac.setEmail("vezbac@example.com");

        vezba = new Vezba();
        vezba.setId(10L);
        vezba.setNaziv("Bench Press");

        planVezbeRequestItem = new PlanVezbeRequestItem(10L, 4, 12);
        planVezbe = new PlanVezbe();
        planVezbe.setId(1L);
        planVezbe.setVezba(vezba);
        planVezbe.setBrojSerija(4);
        planVezbe.setBrojPonavljanja(12);

        planTreninga = new PlanTreninga();
        planTreninga.setId(100L);
        planTreninga.setVezbac(vezbac);
        planTreninga.setNaziv("Plan za grudi");
        planTreninga.setDatum(LocalDateTime.now());
        planTreninga.setPlanoviVezbi(List.of(planVezbe));
        planVezbe.setPlanTreninga(planTreninga);

        planTreningaRequest = new PlanTreningaRequest("Plan za grudi", List.of(planVezbeRequestItem));
        
        planVezbeResponse = new PlanVezbeResponse();
        planVezbeResponse.setId(1L);
        planVezbeResponse.setVezba(new VezbaResponse());
        planVezbeResponse.getVezba().setNaziv("Bench Press");
        planVezbeResponse.setBrojSerija(4);
        planVezbeResponse.setBrojPonavljanja(12);

        planTreningaResponse = new PlanTreningaResponse();
        planTreningaResponse.setId(100L);
        planTreningaResponse.setNaziv("Plan za grudi");
        planTreningaResponse.setDatum(planTreninga.getDatum());
        planTreningaResponse.setPlanoviVezbi(List.of(planVezbeResponse));

    }
    
    /**
     * Pomoćna metoda za simuliranje autentifikovanog korisnika.
     */
    private void mockAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(vezbac.getEmail());
        when(userRepository.findByEmail(vezbac.getEmail())).thenReturn(Optional.of(vezbac));
    }
    
    // --- TESTOVI ZA getAllPlanoviTreninga() ---

   @Test
void testGetAllPlanoviTreninga_Success() {
    try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        mockAuthenticatedUser();

        // Ključna ispravka: Kreira se Pageable objekat sa istim sortom kao u servisnoj metodi
        Pageable pageable = PageRequest.of(0, 10, Sort.by("datum").descending());
        Page<PlanTreninga> planoviPage = new PageImpl<>(List.of(planTreninga), pageable, 1);
        Page<PlanTreningaResponse> responsePage = new PageImpl<>(List.of(planTreningaResponse), pageable, 1);

        when(planTreningaRepository.findByVezbac(vezbac, pageable)).thenReturn(planoviPage);
        when(planTreningaMapper.toPlanTreningaResponse(any(PlanTreninga.class))).thenReturn(planTreningaResponse);

        Page<PlanTreningaResponse> result = planTreningaService.getAllPlanoviTreninga(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(planTreningaResponse.getNaziv(), result.getContent().get(0).getNaziv());
        verify(planTreningaRepository, times(1)).findByVezbac(vezbac, pageable);
    }
}

    @Test
    void testGetAllPlanoviTreninga_Unauthorized() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThrows(UnauthorizedAccessException.class, () -> planTreningaService.getAllPlanoviTreninga(0, 10));
            verify(planTreningaRepository, never()).findByVezbac(any(), any());
        }
    }
    
    // --- TESTOVI ZA getPlanTreningaById() ---

    @Test
    void testGetPlanTreningaById_Success() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthenticatedUser();

            when(planTreningaRepository.findById(100L)).thenReturn(Optional.of(planTreninga));
            when(planTreningaMapper.toPlanTreningaResponse(planTreninga)).thenReturn(planTreningaResponse);

            PlanTreningaResponse result = planTreningaService.getPlanTreningaById(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Plan za grudi", result.getNaziv());
            verify(planTreningaRepository, times(1)).findById(100L);
        }
    }

    @Test
    void testGetPlanTreningaById_NotFound() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthenticatedUser();

            when(planTreningaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> planTreningaService.getPlanTreningaById(999L));
        }
    }
    
    @Test
    void testGetPlanTreningaById_UnauthorizedAccess() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthenticatedUser();
            
            // Kreiranje plana drugog korisnika
            User drugiVezbac = new User();
            drugiVezbac.setId(2);
            PlanTreninga tudjiPlan = new PlanTreninga();
            tudjiPlan.setId(200L);
            tudjiPlan.setVezbac(drugiVezbac);

            when(planTreningaRepository.findById(200L)).thenReturn(Optional.of(tudjiPlan));

            assertThrows(UnauthorizedAccessException.class, () -> planTreningaService.getPlanTreningaById(200L));
        }
    }
    
    // --- TESTOVI ZA createPlanTreninga() ---

    @Test
    void testCreatePlanTreninga_Success() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthenticatedUser();

            when(planTreningaMapper.toPlanTreninga(any(PlanTreningaRequest.class))).thenReturn(planTreninga);
            when(vezbaRepository.findById(10L)).thenReturn(Optional.of(vezba));
            when(planVezbeMapper.toPlanVezbe(any(PlanVezbeRequestItem.class))).thenReturn(planVezbe);
            when(planTreningaRepository.save(any(PlanTreninga.class))).thenReturn(planTreninga);
            when(planTreningaMapper.toPlanTreningaResponse(any(PlanTreninga.class))).thenReturn(planTreningaResponse);

            PlanTreningaResponse result = planTreningaService.createPlanTreninga(planTreningaRequest);

            assertNotNull(result);
            assertEquals("Plan za grudi", result.getNaziv());
            assertNotNull(result.getPlanoviVezbi());
            assertEquals(1, result.getPlanoviVezbi().size());
            verify(planTreningaRepository, times(1)).save(planTreninga);
        }
    }

    @Test
    void testCreatePlanTreninga_VezbaNotFound() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthenticatedUser();
            
            // Ispravka: Mock-ovanje toPlanTreninga metode da bi se izbegao NullPointerException
            when(planTreningaMapper.toPlanTreninga(any(PlanTreningaRequest.class))).thenReturn(planTreninga);
            
            // Mock-ovanje da vežba ne postoji
            when(vezbaRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> planTreningaService.createPlanTreninga(planTreningaRequest));
            verify(planTreningaRepository, never()).save(any(PlanTreninga.class));
        }
    }
}
