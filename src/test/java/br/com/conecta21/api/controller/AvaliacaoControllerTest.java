package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.AvaliacaoRespostaDTO;
import br.com.conecta21.api.service.AvaliacaoService;
import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvaliacaoController.class)
class AvaliacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AvaliacaoService avaliacaoService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private AvaliacaoRespostaDTO resposta(Long id, Long chamadoId) {
        return new AvaliacaoRespostaDTO(id, chamadoId, 10L, 5,
                "Ótimo atendimento.", LocalDateTime.of(2026, 9, 11, 10, 0));
    }

    @Test
    @WithMockUser
    void criar_retorna201ComLocationECorpo() throws Exception {
        when(avaliacaoService.criar(eq(5L), any())).thenReturn(resposta(1L, 5L));

        mockMvc.perform(post("/api/chamados/5/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nota", 5,
                                "comentario", "Ótimo atendimento."))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/5/avaliacao"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.chamadoId").value(5))
                .andExpect(jsonPath("$.nota").value(5));
    }

    @Test
    @WithMockUser
    void criar_notaInvalida_retorna400() throws Exception {
        mockMvc.perform(post("/api/chamados/5/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nota":6}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void criar_chamadoNaoResolvido_retorna400() throws Exception {
        when(avaliacaoService.criar(eq(5L), any()))
                .thenThrow(new IllegalArgumentException("Chamado ainda não foi resolvido. Avaliação liberada apenas após RESOLVIDO."));

        mockMvc.perform(post("/api/chamados/5/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nota", 5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void criar_avaliadorNaoESolicitante_retorna403() throws Exception {
        when(avaliacaoService.criar(eq(5L), any()))
                .thenThrow(new AccessDeniedException("Apenas o solicitante do chamado pode avaliar o atendimento."));

        mockMvc.perform(post("/api/chamados/5/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nota", 5))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void criar_avaliacaoDuplicada_retorna409() throws Exception {
        when(avaliacaoService.criar(eq(5L), any()))
                .thenThrow(new IllegalStateException("Este chamado já possui uma avaliação."));

        mockMvc.perform(post("/api/chamados/5/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nota", 4))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void criar_chamadoDeOutroTenant_retorna404() throws Exception {
        when(avaliacaoService.criar(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Chamado não encontrado."));

        mockMvc.perform(post("/api/chamados/99/avaliacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nota", 5))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void buscar_existente_retorna200() throws Exception {
        when(avaliacaoService.buscar(5L)).thenReturn(resposta(1L, 5L));

        mockMvc.perform(get("/api/chamados/5/avaliacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nota").value(5));
    }

    @Test
    @WithMockUser
    void buscar_semAvaliacao_retorna404() throws Exception {
        when(avaliacaoService.buscar(5L)).thenThrow(new EntityNotFoundException("Avaliação não encontrada."));

        mockMvc.perform(get("/api/chamados/5/avaliacao"))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAutenticacao_requisicaoRejeitada() throws Exception {
        mockMvc.perform(get("/api/chamados/5/avaliacao"))
                .andExpect(status().is4xxClientError());
    }
}
