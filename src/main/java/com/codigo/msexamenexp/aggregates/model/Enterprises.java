package com.codigo.msexamenexp.aggregates.model;

import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Enterprises {
    private int idEnterprises;
    private String numDocument;
    private String businessName;
    private String tradeName;
    private int documentsTypeEntity;
    private int enterprisesTypeEntity;
    private int status;

    public static Enterprises fromResponseModel(ResponseSunat responseSunat) {
        Enterprises enterprises = new Enterprises();
        enterprises.setNumDocument(responseSunat.getNumeroDocumento());
        enterprises.setBusinessName(responseSunat.getRazonSocial());
        enterprises.setTradeName(responseSunat.getRazonSocial());
        enterprises.setDocumentsTypeEntity(Integer.parseInt(responseSunat.getTipoDocumento()));
        if (responseSunat.getEstado().equals("ACTIVO")) {
            enterprises.setStatus(Constants.STATUS_ACTIVE);
        } else {
            enterprises.setStatus(Constants.STATUS_INACTIVE);
        }
        return enterprises;
    }
}
