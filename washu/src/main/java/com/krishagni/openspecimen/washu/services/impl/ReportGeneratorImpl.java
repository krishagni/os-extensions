package com.krishagni.openspecimen.washu.services.impl;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
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
			sheet.trackAllColumnsForAutoSizing();

			CellStyle hdTitleLabel = hdTitleLabelStyle(workbook);
			CellStyle hdTitleValue = hdTitleValueStyle(workbook);
			CellStyle hdSubTitleLabel = hdSubTitleLabelStyle(workbook);
			CellStyle hdSubTitleValue = hdSubTitleValueStyle(workbook);

			SXSSFRow hr = sheet.createRow(0);
			CellUtil.createCell(hr, 0, "TPC Project Request#:", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

			CellUtil.createCell(hr, 6, "Exported On", hdTitleLabel);
			CellUtil.createCell(hr, 7, Utility.getDateTimeString(Calendar.getInstance().getTime()), hdTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

			hr = sheet.createRow(1);
			CellUtil.createCell(hr, 0, "Specimens Pulled By (Initials / Date):", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

			CellUtil.createCell(hr, 6, "Exported By", hdTitleLabel);
			CellUtil.createCell(hr, 7, AuthUtil.getCurrentUser().formattedName(), hdTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

			hr = sheet.createRow(2);
			CellUtil.createCell(hr, 0, "Specimens Refiled By (Initials / Date):", hdTitleLabel);
			CellUtil.createCell(hr, 1, "", hdTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 11));

			hr = sheet.createRow(3);
			CellUtil.createCell(hr, 0, "Sample Available Quantity Units:", hdSubTitleLabel);
			CellUtil.createCell(hr, 1, "Cell = cell count/number, Fluid/Tissue Lysate/Cell Lysate = ml, Molecular = ug, Tissue Block/Slide = Count, All Other Tissue = gm", hdSubTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, data.getColumnLabels().length > 11 ? data.getColumnLabels().length - 1 : 11));

			hr = sheet.createRow(4);
			CellUtil.createCell(hr, 0, "Refill Legend:", hdSubTitleLabel);
			CellUtil.createCell(hr, 1, "Y = Yes, N-E = No-Exhausted, N-D = No-Distributed", hdSubTitleValue);
			sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, data.getColumnLabels().length > 11 ? data.getColumnLabels().length - 1 : 11));

			sheet.flushRows();

			hr = sheet.createRow(5);

			SXSSFRow dataRow = sheet.createRow(6);
			int colNum = 0;
			for (String columnLabel : data.getColumnLabels()) {
				CellUtil.createCell(dataRow, colNum++, columnLabel, hdSubTitleLabel);
			}

			CellUtil.createCell(dataRow, colNum++, "Pulled (Y, N)", hdSubTitleLabel);
			CellUtil.createCell(dataRow, colNum++, "Refiled (Y, N-E, N-D)", hdSubTitleLabel);

			sheet.flushRows();

			int rowNum = 7;
			Iterator<String[]> rows = data.stringifiedRowIterator();
			while (rows.hasNext()) {
				dataRow = sheet.createRow(rowNum++);
				colNum = 0;
				for (String item : rows.next()) {
					CellUtil.createCell(dataRow, colNum++, item, hdSubTitleValue);
				}

				CellUtil.createCell(dataRow, colNum++, "", hdSubTitleValue);
				CellUtil.createCell(dataRow, colNum++, "", hdSubTitleValue);

				if ((rowNum - 7) % 10 == 0) {
					sheet.flushRows();
				}
			}

			sheet.flushRows();

			int numOfColumns = data.getColumnLabels().length;
			if (numOfColumns < 12) {
				numOfColumns = 12;
			}

			for (int i = 0; i < numOfColumns; ++i) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(out);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
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
				sheet.trackAllColumnsForAutoSizing();

				CellStyle hdTitleLabel = hdTitleLabelStyle(workbook);
				CellStyle hdTitleValue = hdTitleValueStyle(workbook);
				CellStyle hdSubTitleLabel = hdSubTitleLabelStyle(workbook);
				CellStyle hdSubTitleValue = hdSubTitleValueStyle(workbook);

				int rowNum = -1;
				SXSSFRow hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Order Name:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getName(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Exported On:", hdTitleLabel);
				CellUtil.createCell(hr, 7, Utility.getDateTimeString(Calendar.getInstance().getTime()), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Order ID:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getId().toString(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Exported By:", hdTitleLabel);
				CellUtil.createCell(hr, 7, AuthUtil.getCurrentUser().formattedName(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Distribution Protocol:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getDistributionProtocol().getShortTitle(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Signature & Date:", hdTitleLabel);
				CellUtil.createCell(hr, 1, "", hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Name:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().formattedName(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Distribution Site:", hdTitleLabel);
				CellUtil.createCell(hr, 7, getDistributionSites(order), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requested Date:", hdTitleLabel);
				CellUtil.createCell(hr, 1, Utility.getDateTimeString(order.getCreationDate()), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				String distDate = order.getExecutionDate() != null ? Utility.getDateTimeString(order.getExecutionDate()) : StringUtils.EMPTY;
				CellUtil.createCell(hr, 6, "Distribution Date:", hdTitleLabel);
				CellUtil.createCell(hr, 7, distDate, hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Address:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getAddress(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Distributor's Address:", hdTitleLabel);
				CellUtil.createCell(hr, 7, order.getDistributor().getAddress(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Phone:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getPhoneNumber(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Distributor's Phone:", hdTitleLabel);
				CellUtil.createCell(hr, 7, order.getDistributor().getPhoneNumber(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "Requestor's Email:", hdTitleLabel);
				CellUtil.createCell(hr, 1, order.getRequester().getEmailAddress(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));

				CellUtil.createCell(hr, 6, "Distributor's Email:", hdTitleLabel);
				CellUtil.createCell(hr, 7, order.getDistributor().getEmailAddress(), hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				hr = sheet.createRow(++rowNum);
				CellUtil.createCell(hr, 0, "", hdTitleLabel);
				CellUtil.createCell(hr, 1, "", hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, 5));


				CellUtil.createCell(hr, 6, "Distributor's Comment:", hdTitleLabel);
				CellUtil.createCell(hr, 7, "", hdTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 7, 11));

				if (order.getExtension() != null) {
					Map<String, String> labelValueMap = order.getExtension().getLabelValueMap();
					int attrsCount = 0;
					for (Map.Entry<String, String> labelValue : labelValueMap.entrySet()) {
						int colNum = (attrsCount % 2);
						if (colNum == 0) {
							hr = sheet.createRow(++rowNum);
						}

						CellUtil.createCell(hr, colNum * 6,     labelValue.getKey(), hdTitleLabel);
						CellUtil.createCell(hr, colNum * 6 + 1, labelValue.getValue(), hdTitleValue);
						sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), colNum * 6 + 1, colNum * 6 + 5));
						++attrsCount;
					}
				}

				// empty row
				sheet.createRow(++rowNum);

				hr = sheet.createRow(++rowNum);
				String[] columnLabels = data.getColumnLabels();
				CellUtil.createCell(hr, 0, "Sample Quantity Units:", hdSubTitleLabel);
				CellUtil.createCell(hr, 1, "Cell = cell count/number, Fluid/Tissue Lysate/Cell Lysate = ml, Molecular = ug, Tissue Block/Slide = Count, All Other Tissue = gm", hdSubTitleValue);
				sheet.addMergedRegion(new CellRangeAddress(hr.getRowNum(), hr.getRowNum(), 1, columnLabels.length > 11 ? columnLabels.length - 1 : 11));

				sheet.flushRows();

				// empty row
				sheet.createRow(++rowNum);

				SXSSFRow dataRow = sheet.createRow(++rowNum);
				int colNum = 0;
				for (String columnLabel : columnLabels) {
					CellUtil.createCell(dataRow, colNum++, columnLabel, hdSubTitleLabel);
				}

				sheet.flushRows();

				Iterator<String[]> rows = data.stringifiedRowIterator();
				while (rows.hasNext()) {
					dataRow = sheet.createRow(++rowNum);
					colNum = 0;
					for (String item : rows.next()) {
						CellUtil.createCell(dataRow, colNum++, item, hdSubTitleValue);
					}

					if (rowNum  % 10 == 0) {
						sheet.flushRows();
					}
				}

				sheet.flushRows();

				int numOfColumns = columnLabels.length;
				if (numOfColumns < 12) {
					numOfColumns = 12;
				}

				for (int i = 0; i < numOfColumns; ++i) {
					sheet.autoSizeColumn(i);
				}

				workbook.write(out);
			} catch (Exception e) {
				throw OpenSpecimenException.serverError(e);
			} finally {
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

	private static CellStyle hdTitleLabelStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 12, true);
	}

	private static CellStyle hdTitleValueStyle(SXSSFWorkbook workbook) {
		return createCellStyle(workbook, "Arial", 12, false);
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
