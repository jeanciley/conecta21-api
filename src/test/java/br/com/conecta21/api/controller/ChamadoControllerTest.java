package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.service.ChamadoService;
import br.com.conecta21.api.service.AnexoChamadoService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChamadoService chamadoService;

    @MockBean
    private AnexoChamadoService anexoChamadoService;

    // Dependências do SecurityFilter real (carregado pelo slice): sem header
    // Authorization o filtro apenas segue a cadeia, sem interferir nos testes.
    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private ChamadoRespostaDTO resposta(Long id, Long empresaId) {
        return new ChamadoRespostaDTO(id, empresaId, "Impressora quebrada",
                "Impressora do financeiro não liga", "ABERTO", 10L, null,
                LocalDateTime.of(2026, 9, 5, 10, 0), null);
    }

    @Test
    @WithMockUser
    void criar_retorna201ComLocationECorpo() throws Exception {
        when(chamadoService.criar(any())).thenReturn(resposta(1L, 1L));

        mockMvc.perform(post("/api/chamados").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "titulo", "Impressora quebrada",
                                "descricao", "Impressora do financeiro não liga",
                                "prioridade", "BAIXA"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    @WithMockUser
    void criar_empresaIdEnviadoPeloClienteEIgnorado() throws Exception {
        when(chamadoService.criar(any())).thenReturn(resposta(1L, 1L));

        // O DTO de criação não possui campo de tenant: o JSON extra é ignorado
        // e a empresa é sempre resolvida pelo JWT no Service.
        mockMvc.perform(post("/api/chamados").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"X","descricao":"Y","prioridade":"BAIXA","empresaId":999,"empresa_id":999}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaId").value(1));
    }

    @Test
    @WithMockUser
    void criar_dadosInvalidos_retorna400() throws Exception {
        mockMvc.perform(post("/api/chamados").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"","descricao":"Y"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void listar_retorna200Paginado() throws Exception {
        when(chamadoService.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resposta(1L, 1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/chamados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].empresaId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void listar_comFiltros_repassaParametrosAoService() throws Exception {
        when(chamadoService.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resposta(1L, 1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/chamados")
                        .param("status", "ABERTO")
                        .param("tecnicoId", "7")
                        .param("dataInicio", "2026-09-01T00:00:00")
                        .param("dataFim", "2026-09-10T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].empresaId").value(1));

        verify(chamadoService).listar(eq("ABERTO"), eq(7L), any(), any(), any());
    }

    @Test
    @WithMockUser
    void listar_statusInvalido_retorna400() throws Exception {
        when(chamadoService.listar(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Status inválido. Valores aceitos: ABERTO, EM_ANDAMENTO, RESOLVIDO, EM_ATRASO"));

        mockMvc.perform(get("/api/chamados").param("status", "INVENTADO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void detalhar_existente_retorna200() throws Exception {
        when(chamadoService.detalhar(1L)).thenReturn(resposta(1L, 1L));

        mockMvc.perform(get("/api/chamados/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void detalhar_chamadoDeOutroTenant_retorna404() throws Exception {
        when(chamadoService.detalhar(99L)).thenThrow(new EntityNotFoundException("Chamado não encontrado."));

        mockMvc.perform(get("/api/chamados/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void alterarStatus_valido_retorna200() throws Exception {
        when(chamadoService.alterarStatus(eq(1L), any())).thenReturn(resposta(1L, 1L));

        mockMvc.perform(patch("/api/chamados/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EM_ANDAMENTO"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void alterarStatus_chamadoDeOutroTenant_retorna404() throws Exception {
        when(chamadoService.alterarStatus(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Chamado não encontrado."));

        mockMvc.perform(patch("/api/chamados/99/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVIDO"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAutenticacao_requisicaoRejeitada() throws Exception {
        mockMvc.perform(get("/api/chamados"))
                .andExpect(status().is4xxClientError());
    }
}
