package com.krishagni.openspecimen.uams.events;

public class EpicLookupResult {
	private String queryMrn;

	private Object results;

	private String error;

	public String getQueryMrn() {
		return queryMrn;
	}

	public void setQueryMrn(String queryMrn) {
		this.queryMrn = queryMrn;
	}

	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
