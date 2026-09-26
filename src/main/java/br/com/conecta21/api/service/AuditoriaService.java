package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AuditoriaRespostaDTO;
import br.com.conecta21.api.model.LogAuditoria;
import br.com.conecta21.api.repository.LogAuditoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private LogAuditoriaRepository auditoriaRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<AuditoriaRespostaDTO> listarHistorico(String entidade, Long entidadeId){
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        List<LogAuditoria> logs = auditoriaRepository
                .findByEmpresaIdAndEntidadeAndEntidadeIdOrderByDataCriacaoDesc(empresaId, entidade, entidadeId);

        return logs.stream()
                .map(this::toRespostaDTO)
                .toList();
    }

    private AuditoriaRespostaDTO toRespostaDTO(LogAuditoria log) {
        return new AuditoriaRespostaDTO(
                log.getId(),
                log.getNomeUsuario(),
                log.getAcao(),
                log.getDetalhes(),
                log.getDataCriacao()
        );
    }
}
