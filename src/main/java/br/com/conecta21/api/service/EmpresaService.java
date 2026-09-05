package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.EmpresaCadastroDTO;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
        admin.setPerfil("ADMIN");

        usuarioRepository.save(admin);

        return empresa;
    }
}
