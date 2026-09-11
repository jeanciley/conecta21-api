package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.InteracaoCriacaoDTO;
import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.model.InteracaoChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.InteracaoChamadoRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteracaoChamadoServiceTest {

    @Mock
    private InteracaoChamadoRepository interacaoRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private InteracaoChamadoService service;

    private Empresa empresaMock(Long id) {
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(id);
        return empresa;
    }

    private Usuario autorMock(Long id, Empresa empresa) {
        Usuario autor = mock(Usuario.class);
        when(autor.getId()).thenReturn(id);
        when(autor.getEmpresa()).thenReturn(empresa);
        when(autor.getNome()).thenReturn("Técnico");
        return autor;
    }

    private Chamado chamadoMock(Long id, Empresa empresa) {
        Chamado chamado = mock(Chamado.class);
        when(chamado.getId()).thenReturn(id);
        when(chamado.getEmpresa()).thenReturn(empresa);
        return chamado;
    }

    private InteracaoChamado interacaoReal(Long id, Chamado chamado, Usuario autor) {
        InteracaoChamado interacao = mock(InteracaoChamado.class);
        when(interacao.getId()).thenReturn(id);
        when(interacao.getChamado()).thenReturn(chamado);
        when(interacao.getAutor()).thenReturn(autor);
        when(interacao.getMensagem()).thenReturn("Verificando a impressora.");
        when(interacao.getDataCriacao()).thenReturn(LocalDateTime.of(2026, 9, 10, 10, 0));
        return interacao;
    }

    @Test
    void comentar_chamadoDoTenant_salvaERetorna() {
        Empresa empresaA = empresaMock(1L);
        Usuario autor = autorMock(10L, empresaA);
        Chamado chamado = chamadoMock(5L, empresaA);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(autor);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.getReferenceById(10L)).thenReturn(autor);
        when(interacaoRepository.save(any(InteracaoChamado.class)))
                .thenAnswer(inv -> interacaoReal(1L, chamado, autor));

        InteracaoRespostaDTO resposta = service.comentar(5L, new InteracaoCriacaoDTO("Olá, verificando."));

        verify(interacaoRepository).save(any(InteracaoChamado.class));
        assertEquals(1L, resposta.id());
        assertEquals(5L, resposta.chamadoId());
        assertEquals(10L, resposta.autorId());
    }

    @Test
    void comentar_chamadoDeOutroTenant_lanca404() {
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.comentar(99L, new InteracaoCriacaoDTO("Olá.")));
    }

    @Test
    void listar_chamadoDoTenant_retornaPaginaCronologica() {
        Usuario autor = mock(Usuario.class);
        when(autor.getId()).thenReturn(10L);
        when(autor.getNome()).thenReturn("Técnico");
        Chamado chamado = mock(Chamado.class);
        when(chamado.getId()).thenReturn(5L);
        Pageable pageable = PageRequest.of(0, 20);
        InteracaoChamado interacao = interacaoReal(1L, chamado, autor);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(chamado));
        when(interacaoRepository.findAllByChamadoIdAndChamadoEmpresaIdOrderByDataCriacaoAsc(5L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(interacao)));

        Page<InteracaoRespostaDTO> resposta = service.listar(5L, pageable);

        assertEquals(1, resposta.getTotalElements());
        assertEquals(5L, resposta.getContent().get(0).chamadoId());
    }

    @Test
    void listar_chamadoDeOutroTenant_lanca404() {
        Pageable pageable = PageRequest.of(0, 20);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.listar(99L, pageable));
    }
}
