package br.com.gestao.mapper;

import br.com.gestao.dto.ApiDtos.FornecedorResponse;
import br.com.gestao.entity.Fornecedor;

public final class FornecedorMapper {
    private FornecedorMapper() {}

    public static FornecedorResponse toResponse(Fornecedor fornecedor) {
        return new FornecedorResponse(fornecedor.getId(), fornecedor.getCodigo(), fornecedor.getRazaoSocial(),
                fornecedor.getNomeFantasia(), fornecedor.getCpfCnpj(), fornecedor.getEmail(), fornecedor.getTelefone(),
                fornecedor.getEndereco(), fornecedor.getCidade(), fornecedor.getUf(), fornecedor.getCep(),
                fornecedor.isAtivo(), fornecedor.getCriadoEm());
    }
}
