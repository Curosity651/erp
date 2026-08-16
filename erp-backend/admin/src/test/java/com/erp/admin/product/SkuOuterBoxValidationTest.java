package com.erp.admin.product;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.erp.admin.product.model.dto.SkuCreateDTO;
import com.erp.admin.product.model.dto.SkuUpdateDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkuOuterBoxValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void createRejectsMissingOuterBoxData() {
		SkuCreateDTO dto = validCreate();
		dto.setOuterLengthMm(null);

		Set<ConstraintViolation<SkuCreateDTO>> violations = validator.validate(dto);

		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("outerLengthMm"));
	}

	@Test
	void createRejectsZeroAndNegativeOuterBoxData() {
		SkuCreateDTO dto = validCreate();
		dto.setOuterWidthMm(0);
		dto.setOuterHeightMm(-1);
		dto.setOuterGrossWeightG(0);

		Set<ConstraintViolation<SkuCreateDTO>> violations = validator.validate(dto);

		assertThat(violations).extracting(v -> v.getPropertyPath().toString())
			.contains("outerWidthMm", "outerHeightMm", "outerGrossWeightG");
	}

	@Test
	void updateRejectsMissingOuterBoxData() {
		SkuUpdateDTO dto = validUpdate();
		dto.setOuterGrossWeightG(null);

		Set<ConstraintViolation<SkuUpdateDTO>> violations = validator.validate(dto);

		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("outerGrossWeightG"));
	}

	@Test
	void validOuterBoxDataPasses() {
		assertThat(validator.validate(validCreate())).isEmpty();
		assertThat(validator.validate(validUpdate())).isEmpty();
	}

	private SkuCreateDTO validCreate() {
		SkuCreateDTO dto = new SkuCreateDTO();
		dto.setSkuCode("BOX-001");
		dto.setSkuNo(1);
		dto.setSpuCode("SPU-001");
		dto.setOuterLengthMm(1200);
		dto.setOuterWidthMm(600);
		dto.setOuterHeightMm(300);
		dto.setOuterGrossWeightG(25000);
		return dto;
	}

	private SkuUpdateDTO validUpdate() {
		SkuUpdateDTO dto = new SkuUpdateDTO();
		dto.setId(1L);
		dto.setSkuCode("BOX-001");
		dto.setSkuNo(1);
		dto.setSpuCode("SPU-001");
		dto.setOuterLengthMm(1200);
		dto.setOuterWidthMm(600);
		dto.setOuterHeightMm(300);
		dto.setOuterGrossWeightG(25000);
		return dto;
	}

}
