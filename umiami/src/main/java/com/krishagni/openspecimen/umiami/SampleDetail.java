package com.krishagni.openspecimen.umiami;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.Mergeable;

public class SampleDetail implements Mergeable<String, SampleDetail>, Serializable {
	private List<SampleDetail> children = new ArrayList<>();
	
	private String parent;
	
	private String name;
	
	private String matrixType;
	
	private BigDecimal amount;
	
	private String localExtId;
	
	private String depleted;
	
	private BigDecimal concByNano;
	
	private String concByNanoUnit;
	
	private BigDecimal concByOther;
	
	private String concByOtherUnit;
	
	private BigDecimal concByQubit;
	
	private String concByQubitUnit;
	
	private String qubitMethod;
	
	private BigDecimal avg260280;
	
	private String processingComments;
	
	private String hashDigest;

	public List<SampleDetail> getChildren() {
		return children;
	}

	public void setChildren(List<SampleDetail> children) {
		this.children = children;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMatrixType() {
		return matrixType;
	}

	public void setMatrixType(String matrixType) {
		this.matrixType = matrixType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public String getLocalExtId() {
		return localExtId;
	}

	public void setLocalExtId(String localExtId) {
		this.localExtId = localExtId;
	}

	public String getDepleted() {
		return depleted;
	}

	public void setDepleted(String depleted) {
		this.depleted = depleted;
	}

	public BigDecimal getConcByNano() {
		return concByNano;
	}

	public void setConcByNano(BigDecimal concByNano) {
		this.concByNano = concByNano;
	}

	public String getConcByNanoUnit() {
		return concByNanoUnit;
	}

	public void setConcByNanoUnit(String concByNanoUnit) {
		this.concByNanoUnit = concByNanoUnit;
	}

	public BigDecimal getConcByOther() {
		return concByOther;
	}

	public void setConcByOther(BigDecimal concByOther) {
		this.concByOther = concByOther;
	}

	public String getConcByOtherUnit() {
		return concByOtherUnit;
	}

	public void setConcByOtherUnit(String concByOtherUnit) {
		this.concByOtherUnit = concByOtherUnit;
	}

	public BigDecimal getConcByQubit() {
		return concByQubit;
	}

	public void setConcByQubit(BigDecimal concByQubit) {
		this.concByQubit = concByQubit;
	}

	public String getConcByQubitUnit() {
		return concByQubitUnit;
	}

	public void setConcByQubitUnit(String concByQubitUnit) {
		this.concByQubitUnit = concByQubitUnit;
	}

	public String getQubitMethod() {
		return qubitMethod;
	}

	public void setQubitMethod(String qubitMethod) {
		this.qubitMethod = qubitMethod;
	}

	public BigDecimal getAvg260280() {
		return avg260280;
	}

	public void setAvg260280(BigDecimal avg260280) {
		this.avg260280 = avg260280;
	}

	public String getProcessingComments() {
		return processingComments;
	}

	public void setProcessingComments(String processingComments) {
		this.processingComments = processingComments;
	}

	public String getHashDigest() {
		return hashDigest;
	}

	public void setHashDigest(String hashDigest) {
		this.hashDigest = hashDigest;
	}

	@Override
	public String getMergeKey() {
		return getParent();
	}

	@Override
	public void merge(SampleDetail aliquot) {
		children.add(aliquot);
	}
}
