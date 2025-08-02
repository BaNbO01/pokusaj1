/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rs.ac.bg.fon.nst.fitnes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ac.bg.fon.nst.fitnes.domain.KategorijaVezbe;
import rs.ac.bg.fon.nst.fitnes.dto.KategorijaVezbeResponse;
import rs.ac.bg.fon.nst.fitnes.mapper.KategorijaVezbeMapper;
import rs.ac.bg.fon.nst.fitnes.repo.KategorijaVezbeRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Ova klasa sadrži unit testove za {@link KategorijaVezbeService}.
 * Koristi Mockito framework za simuliranje zavisnosti servisa (repozitorijum i maper).
 */
@ExtendWith(MockitoExtension.class)
class KategorijaVezbeServiceTest {

    // Servis koji se testira, injektovaćemo mock objekte u njega.
    @InjectMocks
    private KategorijaVezbeService kategorijaVezbeService;

    // Mock objekti za zavisnosti servisa.
    @Mock
    private KategorijaVezbeRepository kategorijaVezbeRepository;
    @Mock
    private KategorijaVezbeMapper kategorijaVezbeMapper;

    private List<KategorijaVezbe> mockKategorije;
    private List<KategorijaVezbeResponse> mockResponses;

    @BeforeEach
    void setUp() {
        // Inicijalizujemo testne podatke pre svakog testa.
        // Simuliramo listu entiteta iz baze podataka.
        KategorijaVezbe kat1 = new KategorijaVezbe(1L, "Kardio", null, null, null);
        KategorijaVezbe kat2 = new KategorijaVezbe(2L, "Snaga", null, null, null);
        mockKategorije = Arrays.asList(kat1, kat2);

        // Simuliramo listu DTO objekata koje bi maper vratio.
        KategorijaVezbeResponse res1 = new KategorijaVezbeResponse(1L, "Kardio");
        KategorijaVezbeResponse res2 = new KategorijaVezbeResponse(2L, "Snaga");
        mockResponses = Arrays.asList(res1, res2);
    }

    /**
     * Testira metodu {@link KategorijaVezbeService#getAllKategorijeVezbe()}
     * kada repozitorijum vrati listu kategorija.
     */
    @Test
    void testGetAllKategorijeVezbe_Success() {
        // Arrange (Priprema):
        // Definišemo ponašanje mock objekata.
        // Kada se pozove findAll() na repozitorijumu, vrati mockKategorije.
        when(kategorijaVezbeRepository.findAll()).thenReturn(mockKategorije);
        // Kada se pozove toKategorijaVezbeResponseList() na maperu sa mockKategorije, vrati mockResponses.
        when(kategorijaVezbeMapper.toKategorijaVezbeResponseList(mockKategorije)).thenReturn(mockResponses);

        // Act (Akcija):
        // Pozivamo metodu servisa koju testiramo.
        List<KategorijaVezbeResponse> result = kategorijaVezbeService.getAllKategorijeVezbe();

        // Assert (Provera):
        // Proveravamo da li je rezultat validan i da li je jednak očekivanom.
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Kardio", result.get(0).getNaziv());
        assertEquals("Snaga", result.get(1).getNaziv());

        // Proveravamo da li su metode mock objekata pozvane tačno jednom.
        verify(kategorijaVezbeRepository, times(1)).findAll();
        verify(kategorijaVezbeMapper, times(1)).toKategorijaVezbeResponseList(mockKategorije);
    }

    /**
     * Testira metodu {@link KategorijaVezbeService#getAllKategorijeVezbe()}
     * kada je baza podataka prazna.
     */
    @Test
    void testGetAllKategorijeVezbe_EmptyList() {
        // Arrange (Priprema):
        // Definišemo ponašanje mock objekata za slučaj prazne liste.
        when(kategorijaVezbeRepository.findAll()).thenReturn(Collections.emptyList());
        when(kategorijaVezbeMapper.toKategorijaVezbeResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act (Akcija):
        // Pozivamo metodu servisa.
        List<KategorijaVezbeResponse> result = kategorijaVezbeService.getAllKategorijeVezbe();

        // Assert (Provera):
        // Proveravamo da li je rezultat prazna lista.
        assertNotNull(result);
        assertEquals(0, result.size());

        // Proveravamo pozive.
        verify(kategorijaVezbeRepository, times(1)).findAll();
        verify(kategorijaVezbeMapper, times(1)).toKategorijaVezbeResponseList(Collections.emptyList());
    }
}
