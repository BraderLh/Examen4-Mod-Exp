package com.codigo.msexamenexp.service.impl;

import com.codigo.msexamenexp.aggregates.model.Enterprises;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.config.RedisService;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignclient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.repository.EnterprisesTypeRepository;
import com.codigo.msexamenexp.service.EnterprisesService;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class EnterprisesServiceImpl implements EnterprisesService {
    private final DocumentsTypeRepository typeRepository;
    private final EnterprisesTypeRepository enterprisesTypeRepository;
    private final EnterprisesRepository enterprisesRepository;
    private final EnterprisesValidations enterprisesValidations;
    private final RedisService redisService;
    private final SunatClient sunatClient;
    private final Util util;

    @Value("${token.api.sunat}")
    private String tokenSunat;

    @Value("${time.expiration.sunat.info}")
    private String timeExpirationSunatInfo;

    public EnterprisesServiceImpl(EnterprisesTypeRepository enterprisesTypeRepository, EnterprisesRepository enterprisesRepository, EnterprisesValidations enterprisesValidations, DocumentsTypeRepository typeRepository, RedisService redisService, SunatClient sunatClient, Util util) {
        this.enterprisesTypeRepository = enterprisesTypeRepository;
        this.enterprisesRepository = enterprisesRepository;
        this.enterprisesValidations = enterprisesValidations;
        this.typeRepository = typeRepository;
        this.redisService = redisService;
        this.sunatClient = sunatClient;
        this.util = util;
    }

    public EnterprisesEntity getExecutionSunat(String numero){
        String authorizathion = "Bearer " + tokenSunat;
        ResponseSunat responseSunat = sunatClient.getSunatInfo(numero, authorizathion);
        String redisDataResponse = Util.convertResponseToJson(responseSunat);
        Enterprises enterprisesSunat = Enterprises.fromResponseModel(responseSunat);
        EnterprisesEntity enterprisesEntitySunat = EnterprisesEntity.fromEntityModel(enterprisesSunat);
        enterprisesEntitySunat.setDocumentsTypeEntity(typeRepository.findByCodType(String.valueOf("0" + enterprisesSunat.getDocumentsTypeEntity())));
        enterprisesEntitySunat.setEnterprisesTypeEntity(enterprisesTypeRepository.findByCodType("02"));
        redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT_CLIENT + numero, redisDataResponse, Integer.parseInt(timeExpirationSunatInfo));
        return enterprisesEntitySunat;
    }

    @Override
    public ResponseBase createEnterprise(RequestEnterprises requestEnterprises) {
        boolean validate = enterprisesValidations.validateInput(requestEnterprises);
        if(validate){
            EnterprisesEntity enterprises = getEntityCreate(requestEnterprises);
            if (enterprises!=null) {
                enterprisesRepository.save(enterprises);
                return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(enterprises));
            } else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_NON_DATA_SUNAT,Optional.empty());
            }
        }else{
            return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID, Optional.empty());
        }
    }

    @Override
    public ResponseBase findOneEnterprise(String doc) {
        String redisCache = redisService.getValueByKey(Constants.REDIS_KEY_INFO_SUNAT + doc);
        if (redisCache!=null) {
            EnterprisesEntity enterprisesEntity = Util.convertFromJson(redisCache, EnterprisesEntity.class);
            return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterprisesEntity));
        } else {
            EnterprisesEntity enterprisesEntity = enterprisesRepository.findByNumDocument(doc);
            if (enterprisesEntity!=null) {
                return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterprisesEntity));
            } else {
                EnterprisesEntity enterprisesEntitySunat = getExecutionSunat(doc);
                if (enterprisesEntitySunat!=null) {
                    String redisData = Util.convertToJsonEntity(enterprisesEntitySunat);
                    redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT + doc, redisData, Integer.parseInt(timeExpirationSunatInfo));
                    return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterprisesEntitySunat));
                } else {
                    return new ResponseBase(Constants.CODE_ERROR_DATA_NOT, Constants.MESS_NON_DATA_SUNAT, Optional.empty());
                }
            }
        }
    }

    @Override
    public ResponseBase findAllEnterprises() {
        Optional<List<EnterprisesEntity>> allEnterprises = Optional.of(enterprisesRepository.findAll());
        if (allEnterprises.get().isEmpty() || allEnterprises.get().size() == 0) {
            return new ResponseBase(Constants.CODE_ERROR_DATA_NOT, Constants.MESS_ZERO_ROWS, Optional.empty());
        } else {
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, allEnterprises);
        }
    }

    @Override
    public ResponseBase updateEnterprise(Integer id, RequestEnterprises requestEnterprises) {
        if (enterprisesRepository.existsById(id)) {
            Optional<EnterprisesEntity> enterprises = enterprisesRepository.findById(id);
            boolean validationEntity = enterprisesValidations.validateInputUpdate(requestEnterprises);
            if (validationEntity && enterprises.isPresent()) {
                EnterprisesEntity enterprisesUpdate = getEntityUpdate(requestEnterprises, enterprises.get());
                enterprisesRepository.save(enterprisesUpdate);
                return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS_UPDATE, Optional.of(enterprisesUpdate));
            } else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT, Constants.MESS_ERROR_NOT_UPDATE, Optional.empty());
            }
        } else {
            return new ResponseBase(Constants.CODE_ERROR_EXIST, Constants.MESS_NON_DATA_ID, Optional.empty());
        }
    }

    @Override
    public ResponseBase deleteEnterprise(Integer id) {
        if (enterprisesRepository.existsById(id)) {
            Optional<EnterprisesEntity> enterprisesEntity = enterprisesRepository.findById(id);
            if (enterprisesEntity.isPresent()) {
                EnterprisesEntity enterprisesToDelete = getEntityDelete(enterprisesEntity.get());
                enterprisesRepository.save(enterprisesToDelete);
                return new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS_DELETE, Optional.of(enterprisesToDelete));
            } else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT, Constants.MESS_ERROR_NOT_DELETE, Optional.empty());
            }
        } else {
            return new ResponseBase(Constants.CODE_ERROR_EXIST, Constants.MESS_NON_DATA_ID, Optional.empty());
        }
    }

    private EnterprisesEntity getEntityCreate(RequestEnterprises requestEnterprises){
        EnterprisesEntity entity = new EnterprisesEntity();
        EnterprisesEntity enterprisesEntitySunat = getExecutionSunat(requestEnterprises.getNumDocument());
        if (enterprisesEntitySunat != null) {
            entity.setNumDocument(enterprisesEntitySunat.getNumDocument());
            entity.setBusinessName(enterprisesEntitySunat.getBusinessName());
            entity.setTradeName(enterprisesEntitySunat.getTradeName());
            entity.setStatus(Constants.STATUS_ACTIVE);
            entity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
            entity.setDocumentsTypeEntity(getDocumentsType(requestEnterprises));
            entity.setUserCreate(Constants.AUDIT_ADMIN);
            entity.setDateCreate(getTimestamp());
            return entity;
        } else {
            return null;
        }
    }

    private EnterprisesEntity getEntityUpdate(RequestEnterprises requestEnterprises, EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setNumDocument(requestEnterprises.getNumDocument() == null ?
                enterprisesEntity.getNumDocument() : requestEnterprises.getNumDocument());
        enterprisesEntity.setBusinessName(requestEnterprises.getBusinessName() == null ?
                enterprisesEntity.getBusinessName() : requestEnterprises.getBusinessName());
        enterprisesEntity.setTradeName(requestEnterprises.getTradeName() == null ?
                enterprisesEntity.getBusinessName() : requestEnterprises.getTradeName());
        enterprisesEntity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises) == null ?
                enterprisesEntity.getEnterprisesTypeEntity() : getEnterprisesType(requestEnterprises));
        enterprisesEntity.setDocumentsTypeEntity(getDocumentsType(requestEnterprises) == null ?
                enterprisesEntity.getDocumentsTypeEntity() : getDocumentsType(requestEnterprises));
        enterprisesEntity.setUserModif(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateModif(getTimestamp());
        return enterprisesEntity;
    }

    private EnterprisesEntity getEntityDelete(EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setUserDelete(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateDelete(getTimestamp());
        enterprisesEntity.setStatus(Constants.STATUS_INACTIVE);
        return enterprisesEntity;
    }

    private EnterprisesTypeEntity getEnterprisesType(RequestEnterprises requestEnterprises){
        EnterprisesTypeEntity enterprisesTypeEntity = enterprisesTypeRepository.findByCodType(String.valueOf("0" + requestEnterprises.getEnterprisesTypeEntity()));
        return enterprisesTypeEntity != null && requestEnterprises.getEnterprisesTypeEntity() == enterprisesTypeEntity.getIdEnterprisesType() ? enterprisesTypeEntity : null;
    }

    private DocumentsTypeEntity getDocumentsType(RequestEnterprises requestEnterprises){
        DocumentsTypeEntity documentsTypeEntity = typeRepository.findByCodType(Constants.COD_TYPE_RUC);
        return documentsTypeEntity != null && requestEnterprises.getDocumentsTypeEntity() == Integer.parseInt(documentsTypeEntity.getCodType()) ? documentsTypeEntity : null;
    }

    private Timestamp getTimestamp(){
        long currentTime = System.currentTimeMillis();
        return new Timestamp(currentTime);
    }
}
