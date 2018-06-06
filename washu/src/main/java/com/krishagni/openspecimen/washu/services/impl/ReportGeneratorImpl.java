package com.krishagni.openspecimen.washu.services.impl;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.openspecimen.washu.services.ReportGenerator;

import edu.common.dynamicextensions.query.QueryResultData;

public class ReportGeneratorImpl implements ReportGenerator  {

	private SpecimenListService listSvc;

	public void setListSvc(SpecimenListService listSvc) {
		this.listSvc = listSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportWorkingSpecimensReport(RequestEvent<EntityQueryCriteria> req) {
		QueryDataExportResult result = listSvc.exportSpecimenList(req.getPayload(), this::exportToXlsx);
		return ResponseEvent.response(result);
	}

	private void exportToXlsx(QueryResultData data, OutputStream out) {
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
