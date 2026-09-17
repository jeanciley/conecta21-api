package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AvaliacaoCriacaoDTO;
import br.com.conecta21.api.dto.AvaliacaoRespostaDTO;
import br.com.conecta21.api.model.Avaliacao;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.AvaliacaoRepository;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private AvaliacaoService service;

    private Empresa empresaReal(Long id) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNomeFantasia("Empresa Teste");
        return empresa;
    }

    private Usuario usuarioReal(Long id, Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmpresa(empresa);
        usuario.setNome("Solicitante");
        usuario.setEmail("user" + id + "@teste.com");
        return usuario;
    }

    private Chamado chamadoResolvido(Long id, Empresa empresa, Usuario solicitante) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        chamado.setEmpresa(empresa);
        chamado.setSolicitante(solicitante);
        chamado.setTitulo("Impressora quebrada");
        chamado.setDescricao("Impressora do financeiro não liga");
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataFechamento(LocalDateTime.of(2026, 9, 10, 10, 0));
        return chamado;
    }

    private Avaliacao avaliacaoReal(Long id, Chamado chamado, Integer nota, String comentario) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setId(id);
        avaliacao.setChamado(chamado);
        avaliacao.setNota(nota);
        avaliacao.setComentario(comentario);
        avaliacao.setDataCriacao(LocalDateTime.of(2026, 9, 11, 10, 0));
        return avaliacao;
    }

    @Test
    void criar_solicitanteDeChamadoResolvido_salvaERetorna() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(solicitante);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(avaliacaoRepository.existsByChamadoId(5L)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class)))
                .thenAnswer(inv -> avaliacaoReal(1L, chamado, 5, "Ótimo atendimento."));

        AvaliacaoRespostaDTO resposta = service.criar(5L, new AvaliacaoCriacaoDTO(5, "Ótimo atendimento."));

        verify(avaliacaoRepository).save(any(Avaliacao.class));
        assertEquals(1L, resposta.id());
        assertEquals(5, resposta.nota());
        assertEquals(10L, resposta.solicitanteId());
    }

    @Test
    void criar_chamadoNaoResolvido_rejeitadoSemSalvar() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        chamado.setStatus(StatusChamado.EM_ANDAMENTO);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(solicitante);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));

        assertThrows(IllegalArgumentException.class,
                () -> service.criar(5L, new AvaliacaoCriacaoDTO(5, "Boa.")));
        verify(avaliacaoRepository, never()).save(any(Avaliacao.class));
    }

    @Test
    void criar_avaliadorNaoESolicitante_bloqueadoCom403() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Usuario tecnico = usuarioReal(7L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(tecnico);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));

        assertThrows(AccessDeniedException.class,
                () -> service.criar(5L, new AvaliacaoCriacaoDTO(5, "Boa.")));
        verify(avaliacaoRepository, never()).save(any(Avaliacao.class));
    }

    @Test
    void criar_avaliacaoDuplicada_rejeitadaCom409() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(solicitante);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(avaliacaoRepository.existsByChamadoId(5L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.criar(5L, new AvaliacaoCriacaoDTO(4, "Boa.")));
        verify(avaliacaoRepository, never()).save(any(Avaliacao.class));
    }

    @Test
    void criar_chamadoDeOutroTenant_lanca404() {
        Usuario solicitante = usuarioReal(10L, empresaReal(1L));
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(solicitante);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.criar(99L, new AvaliacaoCriacaoDTO(5, "Boa.")));
        verify(avaliacaoRepository, never()).save(any(Avaliacao.class));
    }

    @Test
    void buscar_avaliacaoDoTenant_retornaDados() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        Avaliacao avaliacao = avaliacaoReal(1L, chamado, 5, "Ótimo.");
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(avaliacaoRepository.findByChamadoIdAndChamadoEmpresaId(5L, 1L))
                .thenReturn(Optional.of(avaliacao));

        AvaliacaoRespostaDTO resposta = service.buscar(5L);

        assertEquals(1L, resposta.id());
        assertEquals(5, resposta.nota());
    }

    @Test
    void buscar_chamadoDeOutroTenant_lanca404() {
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.buscar(99L));
    }

    @Test
    void buscar_semAvaliacao_lanca404() {
        Empresa empresaA = empresaReal(1L);
        Usuario solicitante = usuarioReal(10L, empresaA);
        Chamado chamado = chamadoResolvido(5L, empresaA, solicitante);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(avaliacaoRepository.findByChamadoIdAndChamadoEmpresaId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.buscar(5L));
    }
}
