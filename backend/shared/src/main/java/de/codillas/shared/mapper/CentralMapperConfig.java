package de.codillas.shared.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct configuration. Every module mapper uses {@code @Mapper(config =
 * CentralMapperConfig.class)}.
 *
 * <p>DTO-direction mappings must be exhaustive: a new response field nobody mapped breaks the build
 * instead of silently going out as {@code null}. Entity factory / update methods override
 * per-method with {@code @BeanMapping(unmappedTargetPolicy = IGNORE)} — their unmapped targets (id,
 * version, timestamps, defaults) are owned by Hibernate and the builder, not by mapping.
 */
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CentralMapperConfig {}
