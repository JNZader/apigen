package com.example.api.config;

import com.example.api.mappers.GenericMapper;
import org.springframework.context.annotation.Bean;
import com.example.api.entities.*;
import org.springframework.context.annotation.Configuration;

/**
 * MapperConfig es una clase de configuración que define los beans de mapeo para
 * las entidades y sus respectivos DTOs (Data Transfer Objects) en la
 * aplicación. Utiliza la anotación {@link Configuration} para indicar que esta
 * clase puede contener definiciones de beans que serán gestionadas por el
 * contexto de Spring.
 */
@Configuration
public class MapperConfig {

    /*
    // Ejemplo de bean de mapeo para la entidad Autor y su DTO
    @Bean
    public GenericMapper<Autor, AutorDTO> autorMapper() {
        return new GenericMapper<>(Autor.class, AutorDTO.class);
    }
    */
}
