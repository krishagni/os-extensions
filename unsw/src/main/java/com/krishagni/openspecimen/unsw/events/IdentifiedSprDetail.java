package com.krishagni.openspecimen.unsw.events;

import org.springframework.web.multipart.MultipartFile;

public class IdentifiedSprDetail {
	
	private Long visitId;
	
	private MultipartFile spr;

	public Long getVisitId() {
		return visitId;
	}

	public void setVisitId(Long visitId) {
		this.visitId = visitId;
	}

	public MultipartFile getSpr() {
		return spr;
	}

	public void setSpr(MultipartFile spr) {
		this.spr = spr;
	}
}
