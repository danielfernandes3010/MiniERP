package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.Fornecedor;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.FornecedorRepository;
import br.com.gestao.mapper.FornecedorMapper;
import br.com.gestao.util.DocumentoValidator;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class FornecedorService {
    private final FornecedorRepository repository;
    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listar(String query) {
        String q = query == null || query.isBlank() ? null : query.trim();
        return repository.pesquisar(q).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public FornecedorResponse obter(@NonNull Long id) { return response(find(id)); }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        Fornecedor f = new Fornecedor();
        update(f, request, true);
        return response(repository.save(Objects.requireNonNull(f)));
    }

    @Transactional
    public FornecedorResponse atualizar(@NonNull Long id, FornecedorRequest request) {
        Fornecedor f = find(id);
        update(f, request, false);
        return response(repository.save(Objects.requireNonNull(f)));
    }

    @Transactional
    public FornecedorResponse ativar(@NonNull Long id, boolean ativo) {
        Fornecedor f = find(id);
        f.setAtivo(ativo);
        f.setAtualizadoEm(LocalDateTime.now());
        return response(repository.save(Objects.requireNonNull(f)));
    }

    private void update(Fornecedor f, FornecedorRequest r, boolean creating) {
        String document = DocumentoValidator.normalize(r.cpfCnpj());
        if (!DocumentoValidator.valid(document)) throw new BusinessException("CPF/CNPJ inválido.", HttpStatus.BAD_REQUEST);
        repository.findAll().stream().filter(other -> !other.getId().equals(f.getId()))
                .filter(other -> other.getCodigo().equalsIgnoreCase(r.codigo().trim()) || other.getCpfCnpj().equals(document))
                .findFirst().ifPresent(other -> { throw new BusinessException("Código ou CPF/CNPJ já cadastrado."); });
        if (r.razaoSocial().trim().length() < 3) throw new BusinessException("Razão social deve possuir ao menos 3 caracteres.", HttpStatus.BAD_REQUEST);
        f.setCodigo(r.codigo().trim());
        f.setRazaoSocial(r.razaoSocial().trim());
        f.setNomeFantasia(blankToNull(r.nomeFantasia()));
        f.setCpfCnpj(document);
        f.setEmail(r.email().trim().toLowerCase());
        f.setTelefone(blankToNull(r.telefone()));
        f.setEndereco(blankToNull(r.endereco()));
        f.setCidade(blankToNull(r.cidade()));
        f.setUf(blankToNull(r.uf()));
        f.setCep(blankToNull(r.cep()));
        if (creating) {
            f.setAtivo(true);
            f.setCriadoEm(LocalDateTime.now());
        }
        f.setAtualizadoEm(LocalDateTime.now());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Fornecedor find(@NonNull Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fornecedor")); }
    private FornecedorResponse response(Fornecedor f) {
        return FornecedorMapper.toResponse(f);
    }
}
