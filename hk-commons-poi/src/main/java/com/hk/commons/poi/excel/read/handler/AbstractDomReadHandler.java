package com.hk.commons.poi.excel.read.handler;

import com.hk.commons.poi.excel.model.ReadParam;
import com.hk.commons.poi.excel.model.ReadResult;
import com.hk.commons.poi.excel.model.SheetData;
import com.hk.commons.poi.excel.model.Title;
import com.hk.commons.util.CollectionUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Use Dom Parse Excel
 * Dom parse Excel Only a small file can be parsed,if you want to parse to large file,please see AbstractSaxReadHandler
 * </pre>
 *
 * @author: kevin
 * @see AbstractSaxReadHandler
 */
public abstract class AbstractDomReadHandler<T> extends AbstractReadHandler<T> implements DomReadHandler<T> {

    /**
     * 工作薄对象
     */
    protected Workbook workbook;

    protected AbstractDomReadHandler(ReadParam<T> param) {
        super(param);
    }

    @Override
    public final ReadResult<T> process(InputStream in) throws IOException, EncryptedDocumentException, OpenXML4JException {
        workbook = WorkbookFactory.create(in);
        sheetParseInit();
        return processWorkbook();
    }

    @Override
    public final ReadResult<T> process(File file) throws IOException, EncryptedDocumentException, OpenXML4JException {
        workbook = WorkbookFactory.create(file);
        sheetParseInit();
        return processWorkbook();
    }

    /**
     * 解析工作薄并验证
     *
     * @return result
     */
    protected ReadResult<T> processWorkbook() {
        ReadResult<T> result = doProcessWorkbook();
        validate(result);
        return result;
    }

    /**
     * 工作表解析初始化设置
     */
    private void sheetParseInit() {
        int maxSheetsIndex = workbook.getNumberOfSheets() - 1;
        if (readParam.isParseAll()) {
            readParam.setSheetStartIndex(0);
            readParam.setSheetMaxIndex(maxSheetsIndex);
        } else {
            readParam.setSheetMaxIndex(Math.min(readParam.getSheetMaxIndex(), maxSheetsIndex));
        }
    }

    /**
     * 解析工作薄
     *
     * @return result
     */
    protected final ReadResult<T> doProcessWorkbook() {
        ReadResult<T> result = new ReadResult<>();
        for (int index = readParam.getSheetStartIndex(); index <= readParam.getSheetMaxIndex(); index++) {
            final Sheet sheet = workbook.getSheetAt(index);
            if (null != sheet) {
                if (null == result.getTitleList()) {
                    List<Title> titles = parseTitleRow(sheet.getRow(readParam.getTitleRow()));
                    result.setTitleList(titles);
                }
                SheetData<T> sheetData = processSheet(sheet, index);
                result.addSheetData(sheetData);
                if (CollectionUtils.isNotEmpty(sheetData.getErrorLogs())) {
                    result.addErrorLogList(sheetData.getErrorLogs());
                }
            }
        }
        return result;
    }

    /**
     * 返回工作表
     *
     * @return workbook
     */
    public final Workbook getWorkbook() {
        return workbook;
    }

    /**
     * 解析工作表
     *
     * @param sheet      工作表
     * @param sheetIndex 工作表所在索引
     * @return 工作表数据
     */
    protected abstract SheetData<T> processSheet(Sheet sheet, int sheetIndex);

    /**
     * 获取单元格的string 值
     *
     * @param cell 单元格
     * @return 单元格的值
     */
    protected final String getCellValueString(Cell cell) {
        return new DataFormatter().formatCellValue(cell);
    }

    /**
     * 解析标题行
     *
     * @param titleRow 标题行
     * @return 标题结果
     */
    protected final List<Title> parseTitleRow(Row titleRow) {
        List<Title> titleList = new ArrayList<>(titleRow.getLastCellNum());
        titleRow.forEach(cell -> titleList.add(new Title(cell, readParam.getColumnProperties().get(cell.getColumnIndex()))));
        setTitles(titleList);
        return titleList;
    }

    /**
     * 是否为合并单元格
     *
     * @param cell 单元格
     * @return 如果此单元格在合并的范围内, 返回true
     */
    protected final boolean isMerageCell(Cell cell) {
        List<CellRangeAddress> mergedRegions = cell.getSheet().getMergedRegions();
        for (CellRangeAddress cellRangeAddress : mergedRegions) {
            if ((cellRangeAddress.getFirstColumn() <= cell.getColumnIndex()
                    && cellRangeAddress.getLastColumn() >= cell.getColumnIndex())
                    || (cellRangeAddress.getFirstRow() <= cell.getRowIndex()
                    && cellRangeAddress.getLastRow() >= cell.getRowIndex())) {
                return true;
            }
        }
        return false;
    }

}
