package mx.gob.sda.ms_store.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNotFound(NoSuchElementException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso No Encontrado", 
            "El documento solicitado no existe o no tiene permisos de acceso.");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Object> handleSecurity(SecurityException ex) {
        log.error("Violación de seguridad: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso Denegado", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Error en parámetros: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Formato Inválido", "Los identificadores UUID proporcionados no son válidos.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalError(Exception ex) {
        log.error("Error no controlado: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno del Servidor", 
            "Ocurrió un error inesperado al procesar la solicitud.");
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        return new ResponseEntity<>(body, headers, status);
    }
}