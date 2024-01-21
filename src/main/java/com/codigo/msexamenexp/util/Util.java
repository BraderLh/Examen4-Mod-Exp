package com.codigo.msexamenexp.util;

import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
public class Util {
    private Util() {
    }

    public static String convertToJsonEntity(EnterprisesEntity enterprisesEntity) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(enterprisesEntity);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String convertResponseToJson(ResponseSunat responseSunat) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(responseSunat);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static <T> T convertFromJson(String json, Class<T> valueType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
