package org.corpus_tools.peppermodules.toolbox.text.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "family-names", "given-names", "orcid" })
public class Author {

	@JsonProperty("family-names")
	private String familyNames;
	@JsonProperty("given-names")
	private String givenNames;
	@JsonProperty("orcid")
	private String orcid;

	/**
	 * No args constructor for use in serialization
	 *
	 */
	public Author() {
	}

	/**
	 *
	 * @param familyNames
	 * @param orcid
	 * @param givenNames
	 */
	public Author(String familyNames, String givenNames, String orcid) {
		super();
		this.familyNames = familyNames;
		this.givenNames = givenNames;
		this.orcid = orcid;
	}

	@JsonProperty("family-names")
	public String getFamilyNames() {
		return familyNames;
	}

	@JsonProperty("family-names")
	public void setFamilyNames(String familyNames) {
		this.familyNames = familyNames;
	}

	@JsonProperty("given-names")
	public String getGivenNames() {
		return givenNames;
	}

	@JsonProperty("given-names")
	public void setGivenNames(String givenNames) {
		this.givenNames = givenNames;
	}

	@JsonProperty("orcid")
	public String getOrcid() {
		return orcid;
	}

	@JsonProperty("orcid")
	public void setOrcid(String orcid) {
		this.orcid = orcid;
	}

}
