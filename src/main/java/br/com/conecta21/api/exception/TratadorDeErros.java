package br.com.conecta21.api.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Captura erro de senha incorreta no login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> tratarErroBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas. Verifique seu e-mail e senha.");
    }

    // Captura falhas gerais de autenticação (ex: token expirado ou inválido)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> tratarErroAutenticacao() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falha na autenticação. Faça login novamente.");
    }

    // Captura tentativas de salvar dados duplicados no banco (ex: E-mail ou CNPJ que já existem)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> tratarErroDuplicidade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflito de dados: A informação enviada (E-mail ou CNPJ) já está cadastrada no sistema.");
    }
}
