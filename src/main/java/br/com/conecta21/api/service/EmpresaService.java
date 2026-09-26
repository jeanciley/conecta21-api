package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.EmpresaCadastroDTO;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.Prioridade;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.repository.PrioridadeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PrioridadeRepository prioridadeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Empresa cadastrarEmpresaComAdmin(EmpresaCadastroDTO dto) {

        Empresa empresa = new Empresa();
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setCnpj(dto.cnpj());
        empresa =  empresaRepository.save(empresa);

        Usuario admin = new Usuario();
        admin.setEmpresa(empresa);
        admin.setNome(dto.nomeUsuario());
        admin.setSenha(passwordEncoder.encode(dto.senhaUsuario()));
        admin.setEmail(dto.emailUsuario());
        admin.setPerfil(PerfilUsuario.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);

        Prioridade padrao = new Prioridade();
        padrao.setEmpresa(empresa); padrao.setNome("Padrão");
        padrao.setSlaRespostaMinutos(240); padrao.setSlaResolucaoMinutos(2880); padrao.setAtiva(true);
        padrao = prioridadeRepository.save(padrao);

        List<String> categoriasPadrao = List.of("Hardware", "Rede", "Software", "Dúvida Geral");
        for (String nomeCat : categoriasPadrao) {
            Categoria cat = new Categoria();
            cat.setNome(nomeCat);
            cat.setEmpresa(empresa); // Associa à empresa recém-criada
            cat.setPrioridade(padrao);
            cat.setAtiva(true);
            categoriaRepository.save(cat);
        }

        return empresa;
    }
}
