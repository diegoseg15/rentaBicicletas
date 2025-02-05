package es.uv.etse.twcam.backend.business.Services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import es.uv.etse.twcam.backend.business.Rental;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RentalService {
    private static final String FILE_PATH = System.getProperty("user.dir") + "/backend/rentas.json";
    public List<Rental> rentas = new ArrayList<>();
    private final Gson gson = new Gson();

    public RentalService() {
        ensureFileAndFolderExists();
        loadRentalFromFile(); // Cargar rentas desde el archivo JSON
    }

    // Leer las rentas desde el archivo JSON
    public void loadRentalFromFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(FILE_PATH))) {
                // Leer el archivo JSON y convertirlo en lista de objetos Rental
                Type listType = new TypeToken<List<Rental>>() {
                }.getType();
                // Leer el archivo JSON y obtener la lista de rentas
                rentas = gson.fromJson(reader, listType);

                if (rentas == null) {
                    rentas = new ArrayList<>();
                }

                System.out.println("Archivo leído desde: " + FILE_PATH);
            } catch (IOException e) {
                System.err.println("Error leyendo el archivo JSON desde " + FILE_PATH + ": " + e.getMessage());
            } catch (com.google.gson.JsonSyntaxException e) {
                System.err.println("Error al parsear el archivo JSON, el formato no es válido: " + e.getMessage());
            }
        }
    }

    // Verificar que la carpeta y el archivo existen
    public void ensureFileAndFolderExists() {
        File backendFolder = new File(System.getProperty("user.dir") + "/backend");
        if (!backendFolder.exists()) {
            backendFolder.mkdir(); // Crear la carpeta backend si no existe
        }

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("Archivo creado en: " + FILE_PATH);
                    // Inicializa el archivo con la estructura básica
                    try (FileWriter writer = new FileWriter(file)) {
                        // Inicia el archivo con una lista vacía de usuarios
                        writer.write("[]");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error creando el archivo JSON: " + e.getMessage());
            }
        }
    }

    // Guardar usuarios en el archivo JSON
    public void saveRentasToFile() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(rentas, writer);
            System.out.println("Archivo guardado en: " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error guardando el archivo JSON en " + FILE_PATH + ": " + e.getMessage());
        }
    }

    // Método de registro
    public boolean renta(String usuarioId, String bicicletaId, String fechaHoraRecogida, Number distancia,
            String estado) {
        // Verificar si la renta ya está registrada
        boolean rentExists = rentas.stream()
                .anyMatch(rent -> rent.getBicicletaId().equals(bicicletaId) && rent.getUsuarioId().equals(usuarioId)
                        && rent.getFechaHoraRecogida().equals(fechaHoraRecogida)
                        && rent.getDistancia().equals(distancia)
                        && rent.getEstado().equals(estado));

        // Verificar si la renta ya fue devuelta
        Optional<Rental> rentalOptional = rentas.stream()
                .filter(rent -> rent.getBicicletaId().equals(bicicletaId) && rent.getUsuarioId().equals(usuarioId)
                        && rent.getFechaHoraRecogida().equals(fechaHoraRecogida))
                .findFirst();

        String estadoRenta = rentalOptional.map(Rental::getEstado).orElse(null);

        if (rentExists && "devuelta".equals(estadoRenta)) {
            return false; // La renta ya existe y fue devuelta
        }

        // Crear nueva renta y agregarla a la lista
        Rental newRent = new Rental(usuarioId, bicicletaId, fechaHoraRecogida, distancia, estado);
        rentas.add(newRent);
        saveRentasToFile(); // Guardar cambios en el archivo JSON
        return true; // Registro exitoso
    }

    public Rental getRental(String usuarioId, String bicicletaId, String fechaHoraRecogida) {
        return rentas.stream()
                .filter(rent -> rent.getBicicletaId().equals(bicicletaId) && rent.getUsuarioId().equals(usuarioId)
                        && rent.getFechaHoraRecogida().equals(fechaHoraRecogida))
                .findFirst().orElse(null);
    }

    public List<Rental> getRentalAll(String usuarioId) {
        return rentas.stream()
                .filter(rent -> rent.getUsuarioId().equals(usuarioId))
                .collect(Collectors.toList());
    }

    public boolean devolucion(String usuarioId, String bicicletaId, String fechaHoraRecogida, Number distancia,
            String estado) {
        // Verificar si la renta ya está registrada
        Rental rent = getRental(usuarioId, bicicletaId, fechaHoraRecogida);
        if (rent == null) {
            return false; // La renta no existe
        }

        // Actualizar la renta
        rent.setDistancia(distancia);
        rent.setEstado(estado);
        saveRentasToFile(); // Guardar cambios en el archivo JSON
        return true; // Devolución exitosa
    }

}
