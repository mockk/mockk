package com.ninjasquad.springmockk.example

import org.springframework.beans.factory.annotation.Qualifier

/**
 * Custom qualifier for testing.
 *
 * @author Stephane Nicoll
 * @author JB Nizet
 */
@Qualifier
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class CustomQualifier
