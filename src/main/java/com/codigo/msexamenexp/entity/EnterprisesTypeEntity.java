package com.codigo.msexamenexp.entity;

import com.codigo.msexamenexp.entity.common.Audit;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

@NamedQuery(name = "EnterprisesTypeEntity.findByCodType", query = "select et from EnterprisesTypeEntity et where et.codType=:codType")
@Entity
@Getter
@Setter
@Table(name = "enterprises_type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterprisesTypeEntity extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_enterprises_type")
    private int idEnterprisesType;

    @Column(name = "cod_type", length = 45,nullable = false)
    private String codType;

    @Column(name = "desc_type",length = 45,nullable = false)
    private String descType;

    @Column(name = "status",nullable = false)
    private int status;
}
