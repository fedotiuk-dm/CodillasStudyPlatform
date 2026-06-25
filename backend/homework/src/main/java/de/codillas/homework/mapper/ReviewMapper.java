package de.codillas.homework.mapper;

import java.util.UUID;

import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.domain.model.Review;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface ReviewMapper {

  ReviewResponse toResponse(Review entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Review toEntity(CreateReviewRequest request, UUID submissionId, UUID reviewerId);
}
