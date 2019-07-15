package com.krishagni.openspecimen.washu.services.impl;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.openspecimen.washu.services.ReportGenerator;

import edu.common.dynamicextensions.query.QueryResultData;
import edu.common.dynamicextensions.query.WideRowMode;

public class ReportGeneratorImpl implements ReportGenerator  {

	private SpecimenListService listSvc;

	private QueryService querySvc;

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	public void setListSvc(SpecimenListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportWorkingSpecimensReport(RequestEvent<SpecimenListCriteria> req) {
		try {
			QueryDataExportResult result = listSvc.exportSpecimenList(req.getPayload(), this::exportSpecimenListToXlsx);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportOrderReport(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			DistributionOrder order = daoFactory.getDistributionOrderDao().getById(crit.getId());
			if (order == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.NOT_FOUND, crit.getId());
			}

			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			SavedQuery reportQuery = getOrderReportQuery(order);
			Filter filter = new Filter();
			filter.setField("Specimen.specimenOrders.id");
			filter.setOp(Filter.Op.EQ);
			filter.setValues(new String[]{order.getId().toString()});

			ExecuteQueryEventOp queryOp = new ExecuteQueryEventOp();
			queryOp.setDrivingForm("Participant");
			queryOp.setAql(reportQuery.getAql(new Filter[]{filter}));
			queryOp.setWideRowMode(WideRowMode.DEEP.name());
			queryOp.setRunType("Export");

			return ResponseEvent.response(querySvc.exportQueryData(queryOp, exportOrderReportToXlsxFn(order)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportRequestReport(RequestEvent<EntityQueryCriteria> req) {
		try {
			Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "cart_specimens_rpt_query", -1);
			if (queryId == -1) {
				return null;
			}

			SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
			if (query == null) {
				throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
			}

			EntityQueryCriteria crit = req.getPayload();
			String restriction = "Specimen.tkRequests.id = " + crit.getId();
			List<Long> specimenIds = null;
			if (crit.getParams() != null) {
				specimenIds = (List<Long>) crit.getParams().get("specimenIds");
			}

			if (specimenIds != null && !specimenIds.isEmpty()) {
				restriction += " and Specimen.id in (" + StringUtils.join(specimenIds, ",") + ")";
			}

			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			String siteCpRestriction = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(
				siteCps, AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());
			if (StringUtils.isNotBlank(siteCpRestriction)) {
				restriction += " and " + siteCpRestriction;
			}

			ExecuteQueryEventOp op = new ExecuteQueryEventOp();
			op.setDrivingForm("Participant");
			op.setAql(query.getAql(restriction));
			op.setWideRowMode(WideRowMode.DEEP.name());
			op.setRunType("Export");
			return ResponseEvent.response(querySvc.exportQueryData(op, this::exportSpecimenListToXlsx));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private SavedQuery getOrderReportQuery(DistributionOrder order) {
		SavedQuery query = order.getDistributionProtocol().getReport();
		if (query != null) {
			return query;
		}

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "distribution_report_query", -1);
		if (queryId == -1) {
			return null;
		}

		return deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
	}

	private void exportSpecimenListToXlsx(QueryResultData data, OutputStream out) {
		SXSSFWorkbook workbook = new SXSSFWorkbook();

		try {
			SXSSFSheet sheet = workbook.createSheet("Specimens");
			sheet.setDefaultColumnWidth(10);
			sheet.setColumnWidth(0, 8000);
			sheet.setAutobreaks(true);
			sheet.setFitToPage(true);

			PrintSetup ps = sheet.getPrintSetup();
			ps.setLandscape(true);
			ps.setFitWidth((short) 1);

			CellStyle hdTitleLabel = hdTitleLabelStyle(workbook);
			CellStyle hdTitleValue = hdTitleValueStyle(workbook);
			CellStyle hdSubTitleLabel = hdSubTitleLabelStyle(workbook);
			CellStyle hdSubTitleValue = hdSubTitleValueStyle(workbook);

			SXSSFRow hr = sheet.createRow(0);
			CellUtil.createCell(hr, 0, "TPC Project Request#:", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			mergeCells(sheet, hr.getRowNum(), 1, 5);

			CellUtil.createCell(hr, 6, "Exported On", hdTitleLabel);
			CellUtil.createCell(hr, 7, Utility.getDateTimeString(Calendar.getInstance().getTime()), hdTitleValue);
			mergeCells(sheet, hr.getRowNum(), 7, 11);

			hr = sheet.createRow(1);
			CellUtil.createCell(hr, 0, "Specimens Pulled By (Initials / Date):", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			mergeCells(sheet, hr.getRowNum(), 1, 5);

			CellUtil.createCell(hr, 6, "Exported By", hdTitleLabel);
			CellUtil.createCell(hr, 7, AuthUtil.getCurrentUser().formattedName(), hdTitleValue);
			mergeCells(sheet, hr.getRowNum(), 7, 11);

			hr = sheet.createRow(2);
			CellUtil.createCell(hr, 0, "Specimens Refiled By (Initials / Date):", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			mergeCells(sheet, hr.getRowNum(), 1, 11);

			hr = sheet.createRow(3);
			CellUtil.createCell(hr, 0, "Sample Available Quantity Units:", hdSubTitleLabel);
			CellUtil.createCell(hr, 1, "Cell = cell count/number, Fluid/Tissue Lysate/Cell Lysate = ml, Molecular = ug, Tissue Block/Slide = Count, All Other Tissue = gm", hdSubTitleValue);
			mergeCells(sheet, hr.getRowNum(), 1, 11);

			hr = sheet.createRow(4);
			CellUtil.createCell(hr, 0, "Refill Legend:", hdSubTitleLabel);
			CellUtil.createCell(hr, 1, "Y = Yes, N-E = No-Exhausted, N-D = No-Distributed", hdSubTitleValue);
			mergeCells(sheet, hr.getRowNum(), 1, 11);

			sheet.flushRows();

			hr = sheet.createRow(5);

			CellStyle thStyle = hdSubTitleLabelStyle(workbook);
			thStyle.setAlignment(HorizontalAlignment.CENTER);

			CellStyle tdStyle = hdSubTitleValueStyle(workbook);
			tdStyle.setAlignment(HorizontalAlignment.CENTER);

			SXSSFRow dataRow = sheet.createRow(6);
			int colNum = 0;
			for (String columnLabel : data.getColumnLabels()) {
				CellUtil.createCell(dataRow, colNum++, columnLabel, thStyle);
			}

			CellUtil.createCell(dataRow, colNum++, "Pulled (Y, N)", thStyle);
			CellUtil.createCell(dataRow, colNum++, "Refiled (Y, N-E, N-D)", thStyle);

			sheet.flushRows();

			int rowNum = 7;
			Iterator<String[]> rows = data.stringifiedRowIterator();
			while (rows.hasNext()) {
				dataRow = sheet.createRow(rowNum++);
				colNum = 0;
				for (String item : rows.next()) {
					CellUtil.createCell(dataRow, colNum++, item, tdStyle);
				}

				CellUtil.createCell(dataRow, colNum++, "", hdSubTitleValue);
				CellUtil.createCell(dataRow, colNum++, "", hdSubTitleValue);

				if ((rowNum - 7) % 10 == 0) {
					sheet.flushRows();
				}
			}

			sheet.flushRows();
			workbook.write(out);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			try {
				data.close();
			} catch (Exception de) {
				de.printStackTrace();
			}

			try {
				workbook.close();
			} catch (Exception we) {
				we.printStackTrace();
			}
		}
	}

	private BiConsumer<QueryResultData, OutputStream> exportOrderReportToXlsxFn(DistributionOrder order) {
		return (data, out) -> {
			SXSSFWorkbook workbook = new SXSSFWorkbook();

			try {
				SXSSFSheet sheet = workbook.createSheet("Order " + order.getId());
				sheet.setDefaultColumnWidth(10);
				sheet.setColumnWidth(0, 10000);
				sheet.setAutobreaks(true);
				sheet.setFitToPage(true);

				PrintSetup ps = sheet.getPrintSetup();
				ps.setLandscape(true);
				ps.setFitWidth((short) 1);

				CellStyle hdTitleLabel = hdTitleLabelStyle(workbook);
				CellStyle hdTitleValue = hdTitleValueStyle(workbook);
				CellStyle hdSubTitleLabel = hdSubTitleLabelStyle(workbook);
				CellStyle hdSubTitleValue = hdSubTitleValueStyle(workbook);

				int rowNum = -1;
				SXSSFRow hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Order Name:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getName(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Exported On:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, Utility.getDateTimeString(Calendar.getInstance().getTime()), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Order ID:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getId().toString(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Exported By:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, AuthUtil.getCurrentUser().formattedName(), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Distribution Protocol:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getDistributionProtocol().getShortTitle(), hdTitleValue);
				mergeCells(sheet, hr.getRowNum(), 1, 12);

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Signature & Date:", hdTitleLabel);
				CellUtil.createCell(hr, 1, "", hdTitleValue);
				mergeCells(sheet, hr.getRowNum(), hr.getRowNum() + 2, 0, 12);
				rowNum += 2;

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Name:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().formattedName(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Distribution Site:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, getDistributionSites(order), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requested Date:", hdTitleLabel);
				CellUtil.createCell(hr, 1, Utility.getDateTimeString(order.getCreationDate()), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				String distDate = order.getExecutionDate() != null ? Utility.getDateTimeString(order.getExecutionDate()) : StringUtils.EMPTY;
				CellUtil.createCell(hr, 6, "Distribution Date:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, distDate, hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Address:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getAddress(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Distributor's Address:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, order.getDistributor().getAddress(), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Phone:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getPhoneNumber(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Distributor's Phone:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, order.getDistributor().getPhoneNumber(), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Email:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getEmailAddress(), hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());

				CellUtil.createCell(hr, 6, "Distributor's Email:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, order.getDistributor().getEmailAddress(), hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "", hdTitleLabel);
				CellUtil.createCell(hr, 1, "", hdTitleValue);
				mergeLeftValueColumns(sheet, hr.getRowNum());


				CellUtil.createCell(hr, 6, "Distributor's Comment:", hdTitleLabel);
				mergeRightLabelColumns(sheet, hr.getRowNum());
				CellUtil.createCell(hr, 8, "", hdTitleValue);
				mergeRightValueColumns(sheet, hr.getRowNum());

				if (order.getExtension() != null) {
					Map<String, String> labelValueMap = order.getExtension().getLabelValueMap();
					int attrsCount = 0;
					for (Map.Entry<String, String> labelValue : labelValueMap.entrySet()) {
						int colNum = (attrsCount % 2);
						if (colNum == 0) {
							hr = sheet.createRow(++rowNum);
							CellUtil.createCell(hr, 0, labelValue.getKey(), hdTitleLabel);
							CellUtil.createCell(hr, 1, labelValue.getValue(), hdTitleValue);
							mergeLeftValueColumns(sheet, hr.getRowNum());
						} else {
							CellUtil.createCell(hr, 6, labelValue.getKey(), hdTitleLabel);
							mergeRightLabelColumns(sheet, hr.getRowNum());
							CellUtil.createCell(hr, 8, labelValue.getValue(), hdTitleValue);
							mergeRightValueColumns(sheet, hr.getRowNum());
						}

						++attrsCount;
					}
				}

				hr = sheet.createRow(++rowNum);
				String[] columnLabels = data.getColumnLabels();
				CellUtil.createCell(hr, 0, "Sample Quantity Units:", hdSubTitleLabel);
				CellUtil.createCell(hr, 1, "Cell = cell count/number, Fluid/Tissue Lysate/Cell Lysate = ml, Molecular = ug, Tissue Block/Slide = Count, All Other Tissue = gm", hdSubTitleValue);
				mergeCells(sheet, hr.getRowNum(), 1, 12);

				sheet.flushRows();

				// empty row
				sheet.createRow(++rowNum);

				CellStyle thStyle = hdSubTitleLabelStyle(workbook);
				thStyle.setAlignment(HorizontalAlignment.CENTER);

				CellStyle tdStyle = hdSubTitleValueStyle(workbook);
				tdStyle.setAlignment(HorizontalAlignment.CENTER);

				SXSSFRow dataRow = sheet.createRow(++rowNum);
				int colNum = 0;
				for (String columnLabel : columnLabels) {
					CellUtil.createCell(dataRow, colNum++, columnLabel, thStyle);
				}

				sheet.flushRows();

				Iterator<String[]> rows = data.stringifiedRowIterator();
				while (rows.hasNext()) {
					dataRow = sheet.createRow(++rowNum);
					colNum = 0;
					for (String item : rows.next()) {
						CellUtil.createCell(dataRow, colNum++, item, tdStyle);
					}

					if (rowNum  % 10 == 0) {
						sheet.flushRows();
					}
				}

				sheet.flushRows();
				workbook.write(out);
			} catch (Exception e) {
				throw OpenSpecimenException.serverError(e);
			} finally {
				try {
					data.close();
				} catch (Exception de) {
					de.printStackTrace();
				}

				try {
					workbook.close();
				} catch (Exception we) {
					we.printStackTrace();
				}
			}
		};
	}

	private String getDistributionSites(DistributionOrder order) {
		DistributionProtocol dp = order.getDistributionProtocol();
		StringBuilder sites = new StringBuilder();

		for (DpDistributionSite dpSite : dp.getDistributingSites()) {
			if (dpSite.getSite() == null) {
				sites.append("All");
			} else {
				sites.append(dpSite.getSite().getName());
			}

			sites.append(" (").append(dpSite.getInstitute().getName()).append("), ");
		}

		if (sites.length() > 0) {
			sites.delete(sites.length() - 2, sites.length());
		}

		return sites.toString();
	}

	private static void mergeLeftValueColumns(SXSSFSheet sheet, int row) {
		mergeCells(sheet, row, 1, 5);
	}

	private static void mergeRightLabelColumns(SXSSFSheet sheet, int row) {
		mergeCells(sheet, row, 6, 7);
	}

	private static void mergeRightValueColumns(SXSSFSheet sheet, int row) {
		mergeCells(sheet, row, 8, 12);
	}

	private static void mergeCells(SXSSFSheet sheet, int row, int startCol, int endCol) {
		mergeCells(sheet, row, row, startCol, endCol);
	}

	private static void mergeCells(SXSSFSheet sheet, int startRow, int endRow, int startCol, int endCol) {
		CellRangeAddress region = new CellRangeAddress(startRow, endRow, startCol, endCol);
		sheet.addMergedRegion(region);
		RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
	}

	private static CellStyle hdTitleLabelStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 10, true);
	}

	private static CellStyle hdTitleValueStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 10, false);
	}

	private static CellStyle hdSubTitleLabelStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 10, true);
	}

	private static CellStyle hdSubTitleValueStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 10, false);
	}

	private static CellStyle createCellStyle(SXSSFWorkbook workbook, String fontName, int fontSize, boolean bold) {
		CellStyle style = workbook.createCellStyle();

		Font font = workbook.createFont();
		font.setFontName(fontName);
		font.setFontHeightInPoints((short) fontSize);
		font.setBold(bold);
		style.setFont(font);
		style.setWrapText(true); // parameterize

		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return addCellBorders(style);
	}

	private static CellStyle addCellBorders(CellStyle style) {
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		return style;
	}

}
