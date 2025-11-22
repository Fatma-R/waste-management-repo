package com.wastemanagement.backend.config;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mm = new ModelMapper();

        // Example: skip nulls during mapping for patch/update behavior
        mm.getConfiguration().setPropertyCondition(Conditions.isNotNull());

        return mm;
    }
}
