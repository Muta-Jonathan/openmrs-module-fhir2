/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r3;

import static lombok.AccessLevel.PACKAGE;

import javax.annotation.Nonnull;

import java.util.HashSet;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.convertors.conv30_40.Observation30_40;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.annotations.R3Provider;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProviderR3Wrapper;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("observationFhirR3ResourceProvider")
@R3Provider
@Setter(PACKAGE)
public class ObservationFhirResourceProvider implements IResourceProvider {
	
	@Autowired
	private FhirObservationService observationService;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Observation.class;
	}
	
	@Read
	@SuppressWarnings("unused")
	public Observation getObservationById(@IdParam @Nonnull IdType id) {
		org.hl7.fhir.r4.model.Observation observation = observationService.get(id.getIdPart());
		if (observation == null) {
			throw new ResourceNotFoundException("Could not find observation with Id " + id.getIdPart());
		}
		
		return Observation30_40.convertObservation(observation);
	}
	
	@Create
	public MethodOutcome createObservationResource(@ResourceParam Observation observation) {
		return FhirProviderUtils.buildCreate(Observation30_40
		        .convertObservation(observationService.create(Observation30_40.convertObservation(observation))));
	}
	
	@Delete
	public OperationOutcome deleteObservationResource(@IdParam @Nonnull IdType id) {
		observationService.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR3();
	}
	
	@Search
	public IBundleProvider searchObservations(
	        @OptionalParam(name = Observation.SP_ENCOUNTER, chainWhitelist = { "",
	                Encounter.SP_TYPE }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = Observation.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY,
	                Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = Observation.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientParam,
	        @OptionalParam(name = Observation.SP_RELATED_TYPE, chainWhitelist = { "",
	                Observation.SP_CODE }, targetTypes = Observation.class) ReferenceAndListParam hasMemberReference,
	        @OptionalParam(name = Observation.SP_VALUE_CONCEPT) TokenAndListParam valueConcept,
	        @OptionalParam(name = Observation.SP_VALUE_DATE) DateRangeParam valueDateParam,
	        @OptionalParam(name = Observation.SP_VALUE_QUANTITY) QuantityAndListParam valueQuantityParam,
	        @OptionalParam(name = Observation.SP_VALUE_STRING) StringAndListParam valueStringParam,
	        @OptionalParam(name = Observation.SP_DATE) DateRangeParam date,
	        @OptionalParam(name = Observation.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = Observation.SP_CATEGORY) TokenAndListParam category,
	        @OptionalParam(name = Observation.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
	        @IncludeParam(allow = { "Observation:" + Observation.SP_ENCOUNTER, "Observation:" + Observation.SP_PATIENT,
	                "Observation:" + Observation.SP_RELATED_TYPE }) HashSet<Include> includes,
	        @IncludeParam(reverse = true, allow = { "Observation:" + Observation.SP_RELATED_TYPE,
	                "DiagnosticReport:" + DiagnosticReport.SP_RESULT }) HashSet<Include> revIncludes) {
		if (patientParam != null) {
			patientReference = patientParam;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		return new SearchQueryBundleProviderR3Wrapper(observationService.searchForObservations(new ObservationSearchParams(
		        encounterReference, patientReference, hasMemberReference, valueConcept, valueDateParam, valueQuantityParam,
		        valueStringParam, date, code, category, id, lastUpdated, sort, includes, revIncludes)));
	}
	
	@Operation(name = "lastn", idempotent = true, type = Observation.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getLastnObservations(@OperationParam(name = "max") NumberParam max,
	        @OperationParam(name = Observation.SP_SUBJECT) ReferenceAndListParam subjectParam,
	        @OperationParam(name = Observation.SP_PATIENT) ReferenceAndListParam patientParam,
	        @OperationParam(name = Observation.SP_CATEGORY) TokenAndListParam category,
	        @OperationParam(name = Observation.SP_CODE) TokenAndListParam code) {
		if (patientParam != null) {
			subjectParam = patientParam;
		}
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(subjectParam);
		searchParams.setCategory(category);
		searchParams.setCode(code);
		
		return new SearchQueryBundleProviderR3Wrapper(observationService.getLastnObservations(max, searchParams));
	}
	
	/**
	 * The $lastn-encounters operation fetches the observations matching the most recent `N` encounters
	 * corresponding to the specified patients.
	 *
	 * @param max The value of `N`, default value should be one
	 * @param subjectParam The reference to a patient
	 * @param patientParam Another way to reference to a patient
	 * @param code The code(s) to which the observation should belong
	 * @param category The category to which the observation should belong If neither patient nor
	 *            subject is specified, then perform search on all patients,
	 * @return a bundle of observations whose corresponding encounter is among the recent `N` encounters
	 *         for the specified patient
	 */
	@Operation(name = "lastn-encounters", idempotent = true, type = Observation.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getLastnEncountersObservations(@OperationParam(name = "max") NumberParam max,
	        @OperationParam(name = Observation.SP_SUBJECT) ReferenceAndListParam subjectParam,
	        @OperationParam(name = Observation.SP_PATIENT) ReferenceAndListParam patientParam,
	        @OperationParam(name = Observation.SP_CATEGORY) TokenAndListParam category,
	        @OperationParam(name = Observation.SP_CODE) TokenAndListParam code) {
		if (patientParam != null) {
			subjectParam = patientParam;
		}
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(subjectParam);
		searchParams.setCategory(category);
		searchParams.setCode(code);
		
		return new SearchQueryBundleProviderR3Wrapper(observationService.getLastnEncountersObservations(max, searchParams));
	}
}
