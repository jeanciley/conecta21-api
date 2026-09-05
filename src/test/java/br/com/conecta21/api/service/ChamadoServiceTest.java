package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private ChamadoService service;

    private Empresa empresaMock(Long id) {
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(id);
        return empresa;
    }

    private Usuario solicitanteMock(Long usuarioId) {
        Usuario solicitante = mock(Usuario.class);
        when(solicitante.getId()).thenReturn(usuarioId);
        return solicitante;
    }

    private Chamado chamadoReal(Empresa empresa, Usuario solicitante, String status) {
        Chamado chamado = new Chamado();
        chamado.setEmpresa(empresa);
        chamado.setSolicitante(solicitante);
        chamado.setTitulo("Impressora quebrada");
        chamado.setDescricao("Impressora do financeiro não liga");
        chamado.setStatus(status);
        return chamado;
    }

    @Test
    void criar_vinculaChamadoAEmpresaDoUsuarioAutenticado() {
        Empresa empresaA = empresaMock(1L);
        Usuario solicitante = solicitanteMock(10L);
        when(solicitante.getEmpresa()).thenReturn(empresaA);
        when(tenantContext.getUsuarioAutenticado()).thenReturn(solicitante);
        when(empresaRepository.getReferenceById(1L)).thenReturn(empresaA);
        when(usuarioRepository.getReferenceById(10L)).thenReturn(solicitante);
        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChamadoRespostaDTO resposta = service.criar(
                new ChamadoCriacaoDTO("Impressora quebrada", "Impressora do financeiro não liga"));

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertSame(empresaA, captor.getValue().getEmpresa());
        assertSame(solicitante, captor.getValue().getSolicitante());
        assertEquals(ChamadoService.STATUS_ABERTO, captor.getValue().getStatus());
        assertEquals(1L, resposta.empresaId());
        assertEquals(10L, resposta.solicitanteId());
    }

    @Test
    void listar_retornaSomenteChamadosDaEmpresaAutenticada() {
        Empresa empresaA = empresaMock(1L);
        Usuario solicitante = solicitanteMock(10L);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findAllByEmpresaId(1L))
                .thenReturn(List.of(chamadoReal(empresaA, solicitante, "ABERTO")));

        List<ChamadoRespostaDTO> resposta = service.listar();

        verify(chamadoRepository).findAllByEmpresaId(1L);
        assertEquals(1, resposta.size());
        assertEquals(1L, resposta.get(0).empresaId());
    }

    @Test
    void detalhar_chamadoDeOutroTenant_lanca404() {
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.detalhar(99L));
    }

    @Test
    void detalhar_chamadoDoTenant_retornaDados() {
        Empresa empresaA = empresaMock(1L);
        Usuario solicitante = solicitanteMock(10L);
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(chamadoReal(empresaA, solicitante, "ABERTO")));

        ChamadoRespostaDTO resposta = service.detalhar(5L);

        assertEquals(1L, resposta.empresaId());
        assertEquals("ABERTO", resposta.status());
    }

    @Test
    void alterarStatus_chamadoDeOutroTenant_bloqueadoSemSalvar() {
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.alterarStatus(99L, new ChamadoStatusDTO("FECHADO")));
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void alterarStatus_valido_atualizaEPreencheDataFechamento() {
        Empresa empresaA = empresaMock(1L);
        Usuario solicitante = solicitanteMock(10L);
        Chamado existente = chamadoReal(empresaA, solicitante, "ABERTO");
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(existente));

        ChamadoRespostaDTO resposta = service.alterarStatus(5L, new ChamadoStatusDTO("fechado"));

        assertEquals("FECHADO", resposta.status());
        assertNotNull(resposta.dataFechamento());
    }

    @Test
    void alterarStatus_reabrindo_limpaDataFechamento() {
        Empresa empresaA = empresaMock(1L);
        Usuario solicitante = solicitanteMock(10L);
        Chamado existente = chamadoReal(empresaA, solicitante, "FECHADO");
        when(tenantContext.getEmpresaIdAutenticada()).thenReturn(1L);
        when(chamadoRepository.findByIdAndEmpresaId(5L, 1L))
                .thenReturn(Optional.of(existente));

        ChamadoRespostaDTO resposta = service.alterarStatus(5L, new ChamadoStatusDTO("EM_ATENDIMENTO"));

        assertEquals("EM_ATENDIMENTO", resposta.status());
        assertNull(resposta.dataFechamento());
    }

    @Test
    void alterarStatus_invalido_rejeitadoSemBuscarNoBanco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.alterarStatus(5L, new ChamadoStatusDTO("INVENTADO")));
        verify(chamadoRepository, never()).findByIdAndEmpresaId(any(), any());
    }
}
