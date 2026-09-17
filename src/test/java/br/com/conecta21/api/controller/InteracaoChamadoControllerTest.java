package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.service.InteracaoChamadoService;
import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(InteracaoChamadoController.class)
class InteracaoChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InteracaoChamadoService interacaoService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private InteracaoRespostaDTO resposta(Long id, Long chamadoId) {
        return new InteracaoRespostaDTO(id, chamadoId, 10L, "Técnico",
                "Verificando a impressora.", LocalDateTime.of(2026, 9, 10, 10, 0));
    }

    @Test
    @WithMockUser
    void comentar_retorna201ComLocationECorpo() throws Exception {
        when(interacaoService.comentar(eq(5L), any())).thenReturn(resposta(1L, 5L));

        mockMvc.perform(post("/api/chamados/5/interacoes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mensagem", "Verificando a impressora."))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/5/interacoes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.chamadoId").value(5))
                .andExpect(jsonPath("$.mensagem").value("Verificando a impressora."));
    }

    @Test
    @WithMockUser
    void comentar_mensagemEmBranco_retorna400() throws Exception {
        mockMvc.perform(post("/api/chamados/5/interacoes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mensagem":""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void comentar_chamadoDeOutroTenant_retorna404() throws Exception {
        when(interacaoService.comentar(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Chamado não encontrado."));

        mockMvc.perform(post("/api/chamados/99/interacoes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mensagem", "Olá."))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listar_retornaPaginaEmOrdemCronologica() throws Exception {
        when(interacaoService.listar(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(resposta(1L, 5L), resposta(2L, 5L)), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/chamados/5/interacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].chamadoId").value(5))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    void listar_chamadoDeOutroTenant_retorna404() throws Exception {
        when(interacaoService.listar(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Chamado não encontrado."));

        mockMvc.perform(get("/api/chamados/99/interacoes"))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAutenticacao_requisicaoRejeitada() throws Exception {
        mockMvc.perform(get("/api/chamados/5/interacoes"))
                .andExpect(status().is4xxClientError());
    }
}
