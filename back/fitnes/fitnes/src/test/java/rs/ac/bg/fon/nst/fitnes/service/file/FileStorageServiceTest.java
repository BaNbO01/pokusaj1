package rs.ac.bg.fon.nst.fitnes.service.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import rs.ac.bg.fon.nst.fitnes.exception.FileUploadException;
import rs.ac.bg.fon.nst.fitnes.exception.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ova klasa sadrži integracione testove za FileStorageService.
 * Koristi @SpringBootTest kako bi učitala ceo Spring kontekst.
 * @TestPropertySource anotacija predefiniše putanju za upload fajlova na
 * privremenu lokaciju koja je specifična za testove, čime se izbegava
 * rad sa stvarnim fajlovima i direktorijumima.
 */
@SpringBootTest
@ActiveProfiles("test")
// Predefinišemo putanju za upload fajlova da bude unutar privremenog direktorijuma.
// System.getProperty("java.io.tmpdir") vraća putanju do privremenog direktorijuma sistema.
@TestPropertySource(properties = "file.upload-dir=target/test-uploads")
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    // Putanja do privremenog test direktorijuma.
    private Path testUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        // Inicijalizujemo putanju do privremenog direktorijuma.
        this.testUploadDir = Paths.get("target/test-uploads");
        // Brišemo direktorijum ako postoji od prethodnog testa kako bi se obezbedila čista okolina.
        if (Files.exists(this.testUploadDir)) {
            Files.walk(this.testUploadDir)
                 .sorted((p1, p2) -> -p1.compareTo(p2)) // sortira od najdubljeg ka površinskom
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         System.err.println("Greška prilikom brisanja: " + path);
                     }
                 });
        }
        // Kreiramo test direktorijum.
        Files.createDirectories(this.testUploadDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Čistimo za sobom. Brišemo sve fajlove i direktorijume unutar test direktorijuma nakon svakog testa.
        Files.walk(this.testUploadDir)
             .sorted((p1, p2) -> -p1.compareTo(p2))
             .forEach(path -> {
                 try {
                     Files.delete(path);
                 } catch (IOException e) {
                     System.err.println("Greška prilikom brisanja: " + path);
                 }
             });
        // Brišemo sam test direktorijum.
        Files.deleteIfExists(this.testUploadDir);
    }

    @Test
    void testStoreFile_Success() throws IOException {
        // Priprema: Kreiramo lažni (mock) MultipartFile
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "Hello, world!".getBytes()
        );
        String originalFileName = "testUser";

        // Akcija: Čuvamo fajl
        String returnedPath = fileStorageService.storeFile(file, originalFileName);

        // Asercije: Proveravamo rezultat
        assertNotNull(returnedPath);
        assertTrue(returnedPath.startsWith(originalFileName + "/"));

        // Proveravamo da li je fajl zaista kreiran u privremenom direktorijumu
        Path storedFile = testUploadDir.resolve(returnedPath);
        assertTrue(Files.exists(storedFile));
        assertEquals("Hello, world!", Files.readString(storedFile));
    }

    @Test
    void testStoreFile_PathTraversalAttack() {
        // Priprema: Kreiramo fajl sa opasnom putanjom
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../dangerous.txt",
                "text/plain",
                "Hello, hacker!".getBytes()
        );
        String originalFileName = "testUser";

        // Asercija: Očekujemo izuzetak
        assertThrows(FileUploadException.class, () -> {
            fileStorageService.storeFile(file, originalFileName);
        });
    }

    @Test
    void testLoadFileAsResource_Success() throws IOException {
        // Priprema: Prvo sačuvamo fajl da bismo ga kasnije učitali
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dummy.txt",
                "text/plain",
                "Test content".getBytes()
        );
        String originalFileName = "testUser";
        String storedPath = fileStorageService.storeFile(file, originalFileName);

        // Akcija: Učitavamo fajl kao resurs
        Resource resource = fileStorageService.loadFileAsResource(storedPath);

        // Asercija: Proveravamo da li je resurs validan
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        // ISPRAVLJENO: Upoređujemo samo naziv fajla, ne celu putanju
        String fileName = storedPath.substring(storedPath.lastIndexOf("/") + 1);
        assertEquals(fileName, resource.getFilename());
    }

    @Test
    void testLoadFileAsResource_NotFound() {
        // Asercija: Očekujemo izuzetak jer fajl ne postoji
        assertThrows(ResourceNotFoundException.class, () -> {
            fileStorageService.loadFileAsResource("non_existent_file.jpg");
        });
    }

    @Test
    void testDeleteFile_Success() throws IOException {
        // Priprema: Sačuvamo fajl
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to_be_deleted.txt",
                "text/plain",
                "Delete me".getBytes()
        );
        String originalFileName = "testUser";
        String storedPath = fileStorageService.storeFile(file, originalFileName);

        // Akcija: Brišemo fajl
        boolean deleted = fileStorageService.deleteFile(storedPath);

        // Asercija: Proveravamo da li je brisanje uspešno i da fajl više ne postoji
        assertTrue(deleted);
        assertFalse(Files.exists(testUploadDir.resolve(storedPath)));
    }

    @Test
    void testDeleteFile_NotFound() {
        // Akcija: Pokušavamo da obrišemo nepostojeći fajl
        boolean deleted = fileStorageService.deleteFile("non_existent_file.txt");

        // Asercija: Proveravamo da li je brisanje neuspešno
        assertFalse(deleted);
    }
}
