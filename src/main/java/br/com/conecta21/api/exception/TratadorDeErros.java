package br.com.conecta21.api.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> tratarErro404() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErro400(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
   
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> tratarErroBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas. Verifique seu e-mail e senha.");
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> tratarErroAutenticacao() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falha na autenticação. Faça login novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> tratarErroDuplicidade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflito de dados: A informação enviada (E-mail ou CNPJ) já está cadastrada no sistema.");
    }
}
