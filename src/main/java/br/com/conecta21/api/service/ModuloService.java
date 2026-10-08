package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ModuloDTO;
import br.com.conecta21.api.dto.ModulosRespostaDTO;
import br.com.conecta21.api.model.EmpresaModulo;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.repository.EmpresaModuloRepository;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModuloService {
    private static final List<CatalogoModulo> CATALOGO = List.of(
            new CatalogoModulo("GMUD", "Gest\u00e3o de Mudan\u00e7as (GMUD)", "Planeje mudan\u00e7as, registre riscos e gere documentos vinculados aos chamados.")
    );

    private final TenantContext tenantContext;
    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository moduloRepository;

    public ModuloService(TenantContext tenantContext, EmpresaRepository empresaRepository, EmpresaModuloRepository moduloRepository) {
        this.tenantContext = tenantContext;
        this.empresaRepository = empresaRepository;
        this.moduloRepository = moduloRepository;
    }

    @Transactional(readOnly = true)
    public ModulosRespostaDTO listar() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Map<String, EmpresaModulo> contratados = moduloRepository.findAllByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(EmpresaModulo::getCodigo, item -> item, (a, b) -> a));
        List<ModuloDTO> modulos = CATALOGO.stream().map(def -> {
            EmpresaModulo registro = contratados.get(def.codigo());
            return new ModuloDTO(def.codigo(), def.nome(), def.descricao(), registro != null && registro.isAtivo(),
                    registro == null ? null : registro.getContratadoEm());
        }).toList();
        boolean gmudAtivo = contratados.containsKey("GMUD") && contratados.get("GMUD").isAtivo();
        return new ModulosRespostaDTO(gmudAtivo, modulos);
    }

    @Transactional
    public ModuloDTO contratar(String codigo) {
        var usuario = tenantContext.getUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuario.ADMIN) {
            throw new AccessDeniedException("Somente administradores podem contratar m\u00f3dulos.");
        }
        String chave = codigo == null ? "" : codigo.trim().toUpperCase();
        CatalogoModulo definicao = CATALOGO.stream().filter(item -> item.codigo().equals(chave)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("M\u00f3dulo n\u00e3o encontrado."));
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        var empresa = empresaRepository.findById(empresaId).orElseThrow(() -> new EntityNotFoundException("Empresa n\u00e3o encontrada."));
        EmpresaModulo registro = moduloRepository.findByEmpresaIdAndCodigo(empresaId, chave).orElseGet(() -> {
            EmpresaModulo novo = new EmpresaModulo();
            novo.setEmpresa(empresa);
            novo.setCodigo(chave);
            return novo;
        });
        if (!registro.isAtivo()) {
            registro.setAtivo(true);
            registro.setContratadoEm(LocalDateTime.now());
        }
        moduloRepository.save(registro);
        return new ModuloDTO(definicao.codigo(), definicao.nome(), definicao.descricao(), registro.isAtivo(), registro.getContratadoEm());
    }

    @Transactional(readOnly = true)
    public boolean estaAtivo(Long empresaId, String codigo) {
        return moduloRepository.findByEmpresaIdAndCodigo(empresaId, codigo.trim().toUpperCase())
                .map(EmpresaModulo::isAtivo).orElse(false);
    }

    private record CatalogoModulo(String codigo, String nome, String descricao) {}
}
