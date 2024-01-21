package com.codigo.msexamenexp.util;

import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnterprisesValidations {
    private final DocumentsTypeRepository typeRepository;
    private final EnterprisesRepository enterprisesRepository;

    public EnterprisesValidations(DocumentsTypeRepository typeRepository, EnterprisesRepository enterprisesRepository) {
        this.typeRepository = typeRepository;
        this.enterprisesRepository = enterprisesRepository;
    }

    public boolean validateInput(RequestEnterprises requestEnterprises){
        if(requestEnterprises == null){
            return false;
        }

        DocumentsTypeEntity documentType = typeRepository.findByCodType(Constants.COD_TYPE_RUC);
        log.info("DATO: " + Integer.valueOf(documentType.getCodType()) + " DATO2: " + requestEnterprises.getEnterprisesTypeEntity());

        if(requestEnterprises.getDocumentsTypeEntity() != Integer.parseInt(documentType.getCodType())
            || requestEnterprises.getNumDocument().length() != Constants.LENGTH_RUC){
            return false;
        }

        if(isNullOrEmpty(requestEnterprises.getNumDocument())){
            return false;
        }

        return !existsEnterprise(requestEnterprises.getNumDocument());
    }

    public boolean validateInputUpdate(RequestEnterprises requestEnterprises){
        if(requestEnterprises == null){
            return false;
        }
        DocumentsTypeEntity documentType = typeRepository.findByCodType(Constants.COD_TYPE_RUC);
        if(requestEnterprises.getDocumentsTypeEntity() != Integer.parseInt(documentType.getCodType())
                || requestEnterprises.getNumDocument().length() != Constants.LENGTH_RUC){
            return false;
        }

        return !isNullOrEmpty(requestEnterprises.getBusinessName()) || !isNullOrEmpty(requestEnterprises.getNumDocument()) || !isNullOrEmpty(requestEnterprises.getTradeName());
    }

    public boolean existsEnterprise(String numDocument){
        return enterprisesRepository.existsByNumDocument(numDocument);
    }

    public boolean isNullOrEmpty(String data){
        return data == null || data.isEmpty();
    }
}
