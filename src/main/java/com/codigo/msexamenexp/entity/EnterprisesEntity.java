package com.codigo.msexamenexp.entity;

import com.codigo.msexamenexp.aggregates.model.Enterprises;
import com.codigo.msexamenexp.entity.common.Audit;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

@NamedQuery(name="DocumentsTypeEntity.findByNumDocument", query = "select e from EnterprisesEntity e where e.numDocument =: doc")
@NamedQuery(name="DocumentsTypeEntity.existsByNumDocument", query = "select case when count(e)>0 then true else false end from EnterprisesEntity e where e.documentsTypeEntity =: integer")
@Entity
@Getter
@Setter
@Table(name = "enterprises")
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterprisesEntity extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_enterprises")
    private int idEnterprises;

    @Column(name = "num_document", length = 15, nullable = false)
    private String numDocument;

    @Column(name = "business_name",length = 150, nullable = false)
    private String businessName;

    @Column(name = "tradename",length = 150)
    private String tradeName;

    @Column(name = "status",nullable = false)
    private int status;

    @ManyToOne
    @JoinColumn(name = "enterprises_type_id_enterprises_type",nullable = false)
    private EnterprisesTypeEntity enterprisesTypeEntity;

    @ManyToOne
    @JoinColumn(name = "document_type_id_document_type",nullable = false)
    private DocumentsTypeEntity documentsTypeEntity;

    public static EnterprisesEntity fromEntityModel(Enterprises enterprises) {
        EnterprisesEntity enterprisesEntity = new EnterprisesEntity();
        enterprisesEntity.setNumDocument(enterprises.getNumDocument());
        enterprisesEntity.setBusinessName(enterprises.getBusinessName());
        enterprisesEntity.setTradeName(enterprises.getTradeName());
        enterprisesEntity.setStatus(enterprises.getStatus());
        return enterprisesEntity;
    }
}
