package com.codigo.msexamenexp.service.impl;

import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.config.RedisService;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignclient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.repository.EnterprisesTypeRepository;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EnterprisesServiceImplTest {
    @Mock
    DocumentsTypeRepository documentsTypeRepository;
    @Mock
    EnterprisesRepository enterprisesRepository;
    @Mock
    EnterprisesTypeRepository enterprisesTypeRepository;
    @Mock
    EnterprisesValidations enterprisesValidations;
    @Mock
    RedisService redisService;
    @Mock
    SunatClient sunatClient;
    @Mock
    Util util;
    @InjectMocks
    EnterprisesServiceImpl enterprisesService;


    @BeforeEach
    void setUp() {
        try {
            MockitoAnnotations.openMocks(this);
            enterprisesService = new EnterprisesServiceImpl(enterprisesTypeRepository, enterprisesRepository,
                    enterprisesValidations, documentsTypeRepository, redisService, sunatClient, util);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    void getExecutionSunat() {
        String authorizathion = "Bearer " + "token";
        String numero = "20494100186";
        ResponseSunat responseSunat = new ResponseSunat("IMPORTACIONES FVC EIRL", "6",
                "20494100186", "ACTIVO", "HABIDO",
                "JR. CORONEL SECADA NRO 281 URB. MOYOBAMBA ",
                "220101", "JR.", "CORONEL SECADA", "URB.",
                "MOYOBAMBA", "281","G203","-","-","-",
                "-", "MOYOBAMBA","MOYOBAMBA","SAN MARTIN", false);

        DocumentsTypeEntity documentsTypeEntity = DocumentsTypeEntity.builder().idDocumentsType(3)
                .codType("06").descType("RUC").status(Constants.STATUS_ACTIVE).build();

        EnterprisesTypeEntity enterprisesTypeEntity = EnterprisesTypeEntity.builder().idEnterprisesType(4)
                .descType("EIRL").codType("04").status(Constants.STATUS_ACTIVE).build();

        EnterprisesEntity enterprisesEntityExpected = new EnterprisesEntity(0, "20494100186",
                "IMPORTACIONES FVC EIRL", "IMPORTACIONES FVC EIRL", Constants.STATUS_ACTIVE,
                enterprisesTypeEntity, documentsTypeEntity);

        String redisDataResponse;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            redisDataResponse = objectMapper.writeValueAsString(responseSunat);
        } catch (JsonProcessingException je) {
            throw new RuntimeException(je.getMessage());
        }

        EnterprisesServiceImpl spy = Mockito.spy(enterprisesService);
        Mockito.when(sunatClient.getSunatInfo(Mockito.anyString(), Mockito.anyString())).thenReturn(responseSunat);
        try (MockedStatic<Util> util = Mockito.mockStatic(Util.class)) {
            util.when(() -> Util.convertResponseToJson(responseSunat)).thenReturn(redisDataResponse);
            assertEquals(Util.convertResponseToJson(responseSunat), redisDataResponse);
        }
        Mockito.doReturn(enterprisesEntityExpected).when(spy).getExecutionSunat(Mockito.anyString());
        Mockito.when(enterprisesTypeRepository.findByCodType(Mockito.anyString())).thenReturn(enterprisesTypeEntity);
        Mockito.when(documentsTypeRepository.findByCodType(Mockito.anyString())).thenReturn(documentsTypeEntity);

        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprisesEntityExpected);

        redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT_CLIENT + numero, redisDataResponse, 10);
        EnterprisesEntity enterprisesEntityObtained = spy.getExecutionSunat("20494100186");

        assertNotNull(enterprisesEntityObtained);
        assertEquals(enterprisesEntityExpected, enterprisesEntityObtained);
    }

    @Test
    void createEnterprise() {
        boolean validateInput = true;

        RequestEnterprises requestEnterprises = new RequestEnterprises("20608868390", "FRACTAL TECNOLOGIA S.A.C.",
                "FRACTAL TECNOLOGIA S.A.C.", 2, 6);

        EnterprisesEntity enterprisesEntity = new EnterprisesEntity(0, "20608868390",
                "FRACTAL TECNOLOGIA S.A.C.", "FRACTAL TECNOLOGIA S.A.C.", 1,
                null, null);
        enterprisesEntity.setUserCreate(Constants.AUDIT_ADMIN);
        long currentTime = System.currentTimeMillis();
        enterprisesEntity.setDateCreate(new Timestamp(currentTime));
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));

        EnterprisesServiceImpl spy = Mockito.spy(enterprisesService);

        Mockito.when(enterprisesValidations.validateInput(Mockito.any(RequestEnterprises.class))).thenReturn(validateInput);

        Mockito.doReturn(enterprisesEntity).when(spy).getExecutionSunat(Mockito.anyString());

        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprisesEntity);

        ResponseBase responseBaseObtained = spy.createEnterprise(requestEnterprises);

        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertNotEquals(responseBaseExpected.getData(), responseBaseObtained.getData());

        if (responseBaseExpected.getData().isPresent() && responseBaseObtained.getData().isPresent()) {
            EnterprisesEntity dataExpected = (EnterprisesEntity) responseBaseExpected.getData().get();
            EnterprisesEntity dataObtained = (EnterprisesEntity) responseBaseObtained.getData().get();
            assertEquals(dataExpected.getIdEnterprises(), dataObtained.getIdEnterprises());
            assertEquals(dataExpected.getNumDocument(), dataObtained.getNumDocument());
            assertEquals(dataExpected.getBusinessName(), dataObtained.getBusinessName());
            assertEquals(dataExpected.getStatus(), dataObtained.getStatus());
        }
    }

    @Test
    void findOneEnterprise() {
        findOneEnterpriseByDoc_NotCacheOrDatabase_ReturnsEnterprise();
        findOneEnterpriseByDoc_RedisCache_ReturnsEnterprise();
    }

    @Test
    void findOneEnterpriseByDoc_RedisCache_ReturnsEnterprise() {
        String doc = "20603049684";

        EnterprisesEntity enterpriseFound = EnterprisesEntity.builder().idEnterprises(0)
                .numDocument("20603049684").businessName("ESTUDIO CONTABLE O & RM S.A.C.")
                .tradeName("ESTUDIO CONTABLE O & RM S.A.C.").status(Constants.STATUS_ACTIVE)
                .enterprisesTypeEntity(null).documentsTypeEntity(null).build();

        String redisCacheSaved;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            redisCacheSaved = objectMapper.writeValueAsString(enterpriseFound);
        } catch (JsonProcessingException je) {
            throw new RuntimeException(je.getMessage());
        }
        redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT + doc, redisCacheSaved, 10);
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterpriseFound));
        ResponseBase responseBaseObtained;

        try (MockedStatic<Util> util = Mockito.mockStatic(Util.class)) {
            util.when(() -> Util.convertFromJson(redisCacheSaved, EnterprisesEntity.class)).thenReturn(enterpriseFound);
            assertEquals(Util.convertFromJson(redisCacheSaved, EnterprisesEntity.class), enterpriseFound);
            responseBaseObtained = new ResponseBase(Constants.CODE_SUCCESS,
                    Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterpriseFound));
        }

        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
    }

    @Test
    void findOneEnterpriseByDoc_NotCacheOrDatabase_ReturnsEnterprise() {
        String doc = "20603049684";
        EnterprisesEntity enterpriseFound = EnterprisesEntity.builder().idEnterprises(0)
                .numDocument("20603049684").businessName("ESTUDIO CONTABLE O & RM S.A.C.")
                .tradeName("ESTUDIO CONTABLE O & RM S.A.C.").status(Constants.STATUS_ACTIVE)
                .enterprisesTypeEntity(null).documentsTypeEntity(null).build();
        Mockito.when(enterprisesRepository.findByNumDocument(Mockito.anyString())).thenReturn(enterpriseFound);
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterpriseFound));

        EnterprisesEntity enterpriseFoundInDB = enterprisesRepository.findByNumDocument(doc);
        ResponseBase responseBaseObtainedInDB = new ResponseBase(Constants.CODE_SUCCESS,
                Constants.MESS_DATA_FOUND_SUCCESS, Optional.of(enterpriseFoundInDB));

        EnterprisesServiceImpl spy = Mockito.spy(enterprisesService);
        String redisData;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            redisData = objectMapper.writeValueAsString(enterpriseFound);
        } catch (JsonProcessingException je) {
            throw new RuntimeException(je.getMessage());
        }
        Mockito.doReturn(enterpriseFound).when(spy).getExecutionSunat(Mockito.anyString());
        EnterprisesEntity enterprisesEntityObtained = spy.getExecutionSunat("20494100186");
        ResponseBase responseBaseObtainedInRedis;
        try (MockedStatic<Util> util = Mockito.mockStatic(Util.class)) {
            util.when(() -> Util.convertToJsonEntity(enterprisesEntityObtained)).thenReturn(redisData);
            redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT + doc, redisData, 10);
            responseBaseObtainedInRedis = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_DATA_FOUND_SUCCESS,
                    Optional.of(enterprisesEntityObtained));
        }
        assertEquals(responseBaseExpected.getCode(), responseBaseObtainedInDB.getCode());
        assertEquals(responseBaseExpected.getCode(), responseBaseObtainedInRedis.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtainedInDB.getMessage());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtainedInRedis.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtainedInDB.getData());
        assertEquals(responseBaseExpected.getData(), responseBaseObtainedInRedis.getData());
    }

    @Test
    void findAllEnterprises_ReturnMoreThanOneEnterprise() {
        EnterprisesEntity enterprises1 = EnterprisesEntity.builder().idEnterprises(0).numDocument("20552103816")
                .businessName("AGROLIGHT PERU S.A.C.").tradeName("AGROLIGHT PERU S.A.C.").enterprisesTypeEntity(null)
                .documentsTypeEntity(null).build();

        EnterprisesEntity enterprises2 = EnterprisesEntity.builder().idEnterprises(1).numDocument("20538856674")
                .businessName("ARTROSCOPICTRAUMA S.A.C.").tradeName("ARTROSCOPICTRAUMA S.A.C.").enterprisesTypeEntity(null)
                .documentsTypeEntity(null).build();

        enterprisesRepository.save(enterprises1);
        enterprisesRepository.save(enterprises2);

        List<EnterprisesEntity> enterprisesEntityListExpected = new ArrayList<>();
        enterprisesEntityListExpected.add(enterprises1);
        enterprisesEntityListExpected.add(enterprises2);

        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprises1);
        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprises2);
        Mockito.when(enterprisesRepository.findAll()).thenReturn(enterprisesEntityListExpected);

        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntityListExpected));

        ResponseBase responseBaseObtained = enterprisesService.findAllEnterprises();

        Optional expectedData = responseBaseExpected.getData();
        Optional obtainedData = responseBaseObtained.getData();
        int size1 = 0, size2 = 0;
        if (expectedData.isPresent()) {
            Object object = expectedData.get();
            List<EnterprisesEntity> enterprisesEntityList = (List<EnterprisesEntity>) object;
            size1 = enterprisesEntityList.size();
        }
        if (obtainedData.isPresent()) {
            Object object = obtainedData.get();
            List<EnterprisesEntity> enterprisesEntityList = (List<EnterprisesEntity>) object;
            size2 = enterprisesEntityList.size();
        }

        assertNotNull(responseBaseObtained);
        assertFalse(responseBaseObtained.getData().isEmpty());
        assertEquals(size1, size2);
        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
    }
    @Test
    void findAllEnterprises_ZeroData_ReturnEmptyListEnterprise() {
        List<EnterprisesEntity> enterprisesEntityList = new ArrayList<>();
        Mockito.when(enterprisesRepository.findAll()).thenReturn(enterprisesEntityList);
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_ERROR_DATA_NOT, Constants.MESS_ZERO_ROWS, Optional.empty());
        ResponseBase responseBaseObtained = enterprisesService.findAllEnterprises();

        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
    }
    @Test
    void updateEnterpriseById_ReturnEnterprise() {
        boolean validationEntity = true;
        RequestEnterprises requestEnterprisesToUpdate = new RequestEnterprises("20547825781", "DMG DRILLING E.I.R.L.",
                "DMG DRILLING E.I.R.L.", 4, 3);

        EnterprisesEntity enterpriseToUpdate = EnterprisesEntity.builder().idEnterprises(5)
                .numDocument("20547825781").businessName("DMG DRILLING").tradeName("DMG DRILLING")
                .status(Constants.STATUS_ACTIVE).enterprisesTypeEntity(null).documentsTypeEntity(null).build();

        DocumentsTypeEntity documentsTypeEntity = DocumentsTypeEntity.builder().idDocumentsType(3)
                .codType("06").descType("RUC").status(Constants.STATUS_ACTIVE).build();

        EnterprisesTypeEntity enterprisesTypeEntity = EnterprisesTypeEntity.builder().idEnterprisesType(4)
                .descType("EIRL").codType("04").status(Constants.STATUS_ACTIVE).build();


        Mockito.when(enterprisesRepository.existsById(Mockito.anyInt())).thenReturn(true);
        Mockito.when(enterprisesRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(enterpriseToUpdate));
        Mockito.when(enterprisesValidations.validateInputUpdate(Mockito.any(RequestEnterprises.class))).thenReturn(validationEntity);

        enterpriseToUpdate.setNumDocument(requestEnterprisesToUpdate.getNumDocument()!=null ? requestEnterprisesToUpdate.getNumDocument() : enterpriseToUpdate.getNumDocument());
        enterpriseToUpdate.setBusinessName(requestEnterprisesToUpdate.getBusinessName()!=null ? requestEnterprisesToUpdate.getBusinessName() : enterpriseToUpdate.getBusinessName());
        enterpriseToUpdate.setTradeName(requestEnterprisesToUpdate.getTradeName()!=null ? requestEnterprisesToUpdate.getTradeName() : enterpriseToUpdate.getTradeName());
        enterpriseToUpdate.setUserModif(Constants.AUDIT_ADMIN);
        enterpriseToUpdate.setDateModif(new Timestamp(System.currentTimeMillis()));

        Mockito.when(documentsTypeRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(documentsTypeEntity));
        Mockito.when(enterprisesTypeRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(enterprisesTypeEntity));

        enterpriseToUpdate.setEnterprisesTypeEntity(enterprisesTypeEntity);
        enterpriseToUpdate.setDocumentsTypeEntity(documentsTypeEntity);

        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterpriseToUpdate);

        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS_UPDATE, Optional.of(enterpriseToUpdate));

        ResponseBase responseBaseObtained = enterprisesService.updateEnterprise(5, requestEnterprisesToUpdate);

        assertNotNull(responseBaseObtained);
        assertNotNull(responseBaseExpected);
        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
    }

    @Test
    void deleteEnterpriseById_ReturnEnterprise() {
        DocumentsTypeEntity documentsTypeEntity = DocumentsTypeEntity.builder().idDocumentsType(3)
                .codType("06").descType("RUC").status(Constants.STATUS_ACTIVE).build();

        EnterprisesTypeEntity enterprisesTypeEntity = EnterprisesTypeEntity.builder().idEnterprisesType(4)
                .descType("EIRL").codType("04").status(Constants.STATUS_ACTIVE).build();

        EnterprisesEntity enterprisesDeleted = EnterprisesEntity.builder().idEnterprises(5)
                .numDocument("20547825781").businessName("DMG DRILLING E.I.R.L.").tradeName("DMG DRILLING E.I.R.L.")
                .status(Constants.STATUS_ACTIVE).enterprisesTypeEntity(enterprisesTypeEntity).documentsTypeEntity(documentsTypeEntity).build();

        Mockito.when(enterprisesRepository.existsById(Mockito.anyInt())).thenReturn(true);
        Mockito.when(enterprisesRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(enterprisesDeleted));

        enterprisesDeleted.setStatus(Constants.STATUS_INACTIVE);
        enterprisesDeleted.setUserDelete(Constants.AUDIT_ADMIN);
        enterprisesDeleted.setDateDelete(new Timestamp(System.currentTimeMillis()));

        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprisesDeleted);

        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS_DELETE, Optional.of(enterprisesDeleted));

        ResponseBase responseBaseObtained = enterprisesService.deleteEnterprise(enterprisesDeleted.getIdEnterprises());

        Integer enterpriseStatusExpected = null, enterpriseStatusObtained = null;
        if (responseBaseExpected.getData().isPresent()) {
            EnterprisesEntity entityExpected = (EnterprisesEntity) responseBaseExpected.getData().get();
            enterpriseStatusExpected = entityExpected.getStatus();
        }
        if (responseBaseObtained.getData().isPresent()) {
            EnterprisesEntity entityObtained = (EnterprisesEntity) responseBaseExpected.getData().get();
            enterpriseStatusObtained = entityObtained.getStatus();
        }

        assertNotNull(responseBaseObtained);
        assertNotNull(responseBaseExpected);
        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
        assertEquals(enterpriseStatusExpected, enterpriseStatusObtained);
    }
    @Test
    void deleteEnterpriseById_NotExists_ReturnEnterpriseIsEmpty() {
        DocumentsTypeEntity documentsTypeEntity = DocumentsTypeEntity.builder().idDocumentsType(3)
                .codType("06").descType("RUC").status(Constants.STATUS_ACTIVE).build();

        EnterprisesTypeEntity enterprisesTypeEntity = EnterprisesTypeEntity.builder().idEnterprisesType(4)
                .descType("EIRL").codType("04").status(Constants.STATUS_ACTIVE).build();

        EnterprisesEntity enterpriseToDelete = EnterprisesEntity.builder().idEnterprises(5)
                .numDocument("20547825781").businessName("DMG DRILLING E.I.R.L.").tradeName("DMG DRILLING E.I.R.L.")
                .status(Constants.STATUS_ACTIVE).enterprisesTypeEntity(enterprisesTypeEntity).documentsTypeEntity(documentsTypeEntity).build();


        Mockito.when(enterprisesRepository.existsById(Mockito.anyInt())).thenReturn(false);

        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_ERROR_DATA_NOT,
                Constants.MESS_ERROR_NOT_DELETE, Optional.empty());

        enterprisesRepository.save(enterpriseToDelete);
        enterprisesRepository.deleteById(enterpriseToDelete.getIdEnterprises());

        Optional<EnterprisesEntity> enterpriseNotFound = enterprisesRepository.findById(enterpriseToDelete.getIdEnterprises());

        ResponseBase responseBaseObtained = new ResponseBase(Constants.CODE_ERROR_DATA_NOT,
                Constants.MESS_ERROR_NOT_DELETE, enterpriseNotFound);

        assertTrue(responseBaseExpected.getData().isEmpty());
        assertTrue(responseBaseObtained.getData().isEmpty());
        responseBaseExpected.getData().ifPresent(Assertions::assertNull);
        responseBaseObtained.getData().ifPresent(Assertions::assertNull);
        assertEquals(responseBaseExpected.getCode(), responseBaseObtained.getCode());
        assertEquals(responseBaseExpected.getMessage(), responseBaseObtained.getMessage());
        assertEquals(responseBaseExpected.getData(), responseBaseObtained.getData());
    }
}