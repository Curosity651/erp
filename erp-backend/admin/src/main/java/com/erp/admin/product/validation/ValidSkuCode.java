package com.erp.admin.product.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * SKU编码格式验证注解
 *
 * @author ballcat
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SkuCodeValidator.class)
@Documented
public @interface ValidSkuCode {

	String message() default "SKU编码格式不正确";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
