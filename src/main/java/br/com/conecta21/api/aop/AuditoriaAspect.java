package br.com.conecta21.api.aop;

import br.com.conecta21.api.model.LogAuditoria;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.LogAuditoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditoriaAspect {

    @Autowired
    private LogAuditoriaRepository auditoriaRepository;

    @Autowired
    private TenantContext tenantContext;

    // Intercepta qualquer método que tenha a anotação @AuditarAcao e que retorne um objeto
    @AfterReturning(pointcut = "@annotation(auditarAcao)", returning = "resultado")
    public void registrarAuditoria(JoinPoint joinPoint, AuditarAcao auditarAcao, Object resultado) {

        // Se não houver retorno ou se não houver usuário logado, não audita
        if (resultado == null || tenantContext.getUsuarioAutenticado() == null) {
            return;
        }

        Usuario autor = tenantContext.getUsuarioAutenticado();
        Long entidadeId = extrairIdDoRetorno(resultado);

        if (entidadeId != null) {
            LogAuditoria log = new LogAuditoria();
            log.setEmpresaId(autor.getEmpresa().getId());
            log.setUsuarioId(autor.getId());
            log.setNomeUsuario(autor.getNome());
            log.setAcao(auditarAcao.acao());
            log.setEntidade(auditarAcao.entidade());
            log.setEntidadeId(entidadeId);

            // Opcional: Você pode inspecionar os argumentos do JoinPoint para gerar detalhes mais ricos
            log.setDetalhes("Ação " + auditarAcao.acao() + " realizada pelo usuário " + autor.getNome());

            auditoriaRepository.save(log);
        }
    }

    /**
     * Tenta invocar o método "id()" ou "getId()" do DTO/Entidade retornado
     * para vincular o log de auditoria ao registro correto.
     */
    private Long extrairIdDoRetorno(Object resultado) {
        try {
            // Tenta para Records (onde o método é public Long id())
            Method recordMethod = resultado.getClass().getMethod("id");
            return (Long) recordMethod.invoke(resultado);
        } catch (Exception e1) {
            try {
                // Tenta para Classes tradicionais (onde o método é public Long getId())
                Method classMethod = resultado.getClass().getMethod("getId");
                return (Long) classMethod.invoke(resultado);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
