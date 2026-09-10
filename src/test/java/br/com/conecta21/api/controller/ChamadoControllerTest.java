package br.com.conecta21.api.controller;

import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.service.AnexoChamadoService;
import br.com.conecta21.api.service.ChamadoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void criarJson_retorna201ComLocationECorpo() throws Exception {
        when(chamadoService.criar(any())).thenReturn(resposta(1L, 1L));

        mockMvc.perform(post("/api/chamados").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "titulo", "Impressora quebrada",
                                "descricao", "Impressora do financeiro não liga"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    @WithMockUser
    void criarMultipartComAnexo_retorna201() throws Exception {
        when(chamadoService.criar(any(), anyList())).thenReturn(resposta(2L, 1L));

        MockMultipartFile dados = new MockMultipartFile(
                "dados",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {"titulo":"Erro 500","descricao":"Segue print e log."}
                """.getBytes()
        );

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivos",
                "erro.log",
                MediaType.TEXT_PLAIN_VALUE,
                "stacktrace".getBytes()
        );

        mockMvc.perform(multipart("/api/chamados")
                        .file(dados)
                        .file(arquivo)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
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
    void listar_retorna200() throws Exception {
        when(chamadoService.listar()).thenReturn(List.of(resposta(1L, 1L)));

        mockMvc.perform(get("/api/chamados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].empresaId").value(1));
    }

    @Test
    @WithMockUser
    void detalhar_chamadoDeOutroTenant_retorna404() throws Exception {
        when(chamadoService.detalhar(99L))
                .thenThrow(new EntityNotFoundException("Chamado não encontrado."));

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
    void semAutenticacao_requisicaoRejeitada() throws Exception {
        mockMvc.perform(get("/api/chamados"))
                .andExpect(status().is4xxClientError());
    }
}
