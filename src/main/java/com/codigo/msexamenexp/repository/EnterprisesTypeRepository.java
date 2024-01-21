package com.codigo.msexamenexp.repository;

import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterprisesTypeRepository extends JpaRepository<EnterprisesTypeEntity, Integer> {
    EnterprisesTypeEntity findByCodType(@Param("codType") String codType);
}
