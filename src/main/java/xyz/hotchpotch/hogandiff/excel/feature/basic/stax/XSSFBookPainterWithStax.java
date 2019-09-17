package xyz.hotchpotch.hogandiff.excel.feature.basic.stax;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers.FilteringReader;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers.PaintColumnsReader;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers.PaintDiffCellsReader;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers.PaintRedundantCellsReader;
import xyz.hotchpotch.hogandiff.excel.feature.basic.stax.readers.PaintRowsReader;
import xyz.hotchpotch.hogandiff.excel.util.BookHandler;
import xyz.hotchpotch.hogandiff.excel.util.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.util.SaxUtil;
import xyz.hotchpotch.hogandiff.excel.util.SaxUtil.SheetInfo;
import xyz.hotchpotch.hogandiff.excel.util.SheetHandler;
import xyz.hotchpotch.hogandiff.excel.util.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.excel.util.StaxUtil.QNAME;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * StAX (Streaming API for XML) を利用して
 * .xlsx/.xlsm 形式のExcelブックのワークシートに着色を行う
 * {@link BookPainter} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class XSSFBookPainterWithStax implements BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * .xlsx/.xlsm 形式のExcelファイルに含まれる xl/styles.xml エントリのラッパーです。<br>
     * 
     * @author nmby
     */
    public static class StylesManager {
        
        // [static members] ----------------------------------------------------
        
        /**
         * {@link StylesManager} オブジェクトを生成して返します。<br>
         * 
         * @param styles xl/styles.xml エントリから生成した {@link Document}
         * @return 新しい {@link StylesManager} オブジェクト
         * @throws NullPointerException {@code styles} が {@code null} の場合
         */
        public static StylesManager of(Document styles) {
            Objects.requireNonNull(styles, "styles");
            return new StylesManager(styles);
        }
        
        // [instance members] --------------------------------------------------
        
        private final Document styles;
        private final Element elemCellXfs;
        private final Element elemFills;
        private final Map<Pair<Integer>, Integer> xfsMap = new HashMap<>();
        private final Map<Short, Integer> fillsMap = new HashMap<>();
        
        private int cellXfsCount;
        private int fillsCount;
        
        private StylesManager(Document styles) {
            assert styles != null;
            this.styles = styles;
            
            elemCellXfs = (Element) styles.getElementsByTagName("cellXfs").item(0);
            elemFills = (Element) styles.getElementsByTagName("fills").item(0);
            cellXfsCount = Integer.parseInt(elemCellXfs.getAttribute("count"));
            fillsCount = Integer.parseInt(elemFills.getAttribute("count"));
        }
        
        /**
         * 指定されたスタイルに指定された色を適用したスタイルのインデックスを返します。<br>
         * 該当するスタイルが既に存在すればそれを、存在しなければ新たに作成して返します。<br>
         * 
         * @param styleIdx 元のスタイルのインデックス
         * @param colorIdx 着色する色のインデックス
         * @return 該当するスタイルのインデックス
         */
        public synchronized int getPaintedStyle(int styleIdx, short colorIdx) {
            Pair<Integer> key = Pair.of(styleIdx, (int) colorIdx);
            if (xfsMap.containsKey(key)) {
                return xfsMap.get(key);
            } else {
                return copyXf(styleIdx, colorIdx);
            }
        }
        
        /**
         * 指定されたスタイルに指定された色を適用したスタイルを新たに作成します。<br>
         * 具体的には、上記を満たす {@code <xf>} 要素を
         * {@code <CellXfs>} 要素の子要素として追加します。<br>
         * 
         * @param styleIdx 元のスタイルのインデックス
         * @param colorIdx 着色する色のインデックス
         * @return 新たなスタイルのインデックス
         */
        private int copyXf(int styleIdx, short colorIdx) {
            xfsMap.put(Pair.of(styleIdx, (int) colorIdx), cellXfsCount);
            cellXfsCount++;
            elemCellXfs.setAttribute("count", Integer.toString(cellXfsCount));
            
            Element originalXf = (Element) elemCellXfs.getElementsByTagName("xf").item(styleIdx);
            Element newXf = (Element) originalXf.cloneNode(true);
            elemCellXfs.appendChild(newXf);
            
            int newFillId = Optional.ofNullable(fillsMap.get(colorIdx))
                    .orElseGet(() -> createFill(colorIdx));
            newXf.setAttribute("fillId", Integer.toString(newFillId));
            newXf.setAttribute("applyFill", "1");
            
            return cellXfsCount - 1;
        }
        
        /**
         * 指定された色の塗りつぶしスタイルを新たに作成します。<br>
         * 具体的には、新たな {@code <fill>} 要素を作成して
         * {@code <fills>} 要素の子要素として追加します。<br>
         * 
         * @param colorIdx 着色する色のインデックス
         * @return
         */
        private int createFill(short colorIdx) {
            fillsMap.put(colorIdx, fillsCount);
            fillsCount++;
            elemFills.setAttribute("count", Integer.toString(fillsCount));
            
            Element newFill = styles.createElement("fill");
            elemFills.appendChild(newFill);
            
            Element patternFill = styles.createElement("patternFill");
            patternFill.setAttribute("patternType", "solid");
            newFill.appendChild(patternFill);
            
            Element fgColor = styles.createElement("fgColor");
            fgColor.setAttribute("indexed", Integer.toString(colorIdx));
            patternFill.appendChild(fgColor);
            
            return fillsCount - 1;
        }
    }
    
    private static final XMLInputFactory inFactory = XMLInputFactory.newInstance();
    private static final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param redundantColor 余剰個所に着ける色のインデックス値
     * @param diffColor 差分個所に着ける色のインデックス値
     * @return 新たなペインター
     */
    public static BookPainter of(
            short redundantColor,
            short diffColor) {
        
        return new XSSFBookPainterWithStax(
                redundantColor,
                diffColor);
    }
    
    // [instance members] ******************************************************
    
    private final short redundantColor;
    private final short diffColor;
    
    private XSSFBookPainterWithStax(
            short redundantColor,
            short diffColor) {
        
        this.redundantColor = redundantColor;
        this.diffColor = diffColor;
    }
    
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public <T> void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            Map<String, Piece<T>> diffs)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(srcBookPath, "srcBookPath");
        Objects.requireNonNull(dstBookPath, "dstBookPath");
        Objects.requireNonNull(diffs, "diffs");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(srcBookPath));
        if (srcBookPath.equals(dstBookPath)) {
            throw new IllegalArgumentException(String.format(
                    "異なるパスを指定する必要があります：%s -> %s", srcBookPath, dstBookPath));
        }
        if (BookType.of(srcBookPath) != BookType.of(dstBookPath)) {
            throw new IllegalArgumentException(String.format(
                    "拡張子が異なります：%s -> %s", srcBookPath, dstBookPath));
        }
        
        // 1. 目的のブックをコピーする。
        copyFile(srcBookPath, dstBookPath);
        
        // 2. 対象のExcelファイルをZipファイルとして扱い各種処理を行う。
        try (FileSystem inFs = FileSystems.newFileSystem(srcBookPath, null);
                FileSystem outFs = FileSystems.newFileSystem(dstBookPath, null)) {
            
            // 2-1. xl/sharedStrings.xml エントリに対する処理
            processSharedStringsEntry(inFs, outFs);
            
            // 2-2. xl/styles.xml エントリに対する処理
            processStylesEntry(inFs, outFs);
            
            // 2-3. xl/worksheets/sheet?.xml エントリに対する処理
            processWorksheetEntries(inFs, outFs, dstBookPath, diffs);
            
        } catch (ExcelHandlingException e) {
            throw e;
        } catch (Exception e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
        
        // TODO 自動生成されたメソッド・スタブ
        
    }
    
    /**
     * 1. 目的のブックをコピーします。<br>
     * 
     * @param src コピー元Excelブックのパス
     * @param dst コピー先のパス（ファイル名を含む）
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    private void copyFile(Path src, Path dst) throws ExcelHandlingException {
        assert src != null;
        assert dst != null;
        assert !src.equals(dst);
        
        try {
            Files.copy(src, dst);
            dst.toFile().setReadable(true, false);
            dst.toFile().setWritable(true, false);
            
        } catch (Exception e) {
            try {
                Files.deleteIfExists(dst);
            } catch (IOException e1) {
                e.addSuppressed(e1);
            }
            throw new ExcelHandlingException(String.format(
                    "Excelファイルのコピーに失敗しました：%s -> %s", src, dst), e);
        }
    }
    
    /**
     * 2-1. xl/sharedStrings.xml エントリに対する処理を行います。<br>
     * 具体的には、当該エントリ内の {@code <color>} 要素を除去することにより、
     * 文字列内の一部の文字に対する着色をクリアします。<br>
     * 
     * @param inFs コピー元Excelブックに対する {@link FileSystem}
     * @param outFs コピー先Excelブックに対する {@link FileSystem}
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    private void processSharedStringsEntry(FileSystem inFs, FileSystem outFs)
            throws ExcelHandlingException {
        
        final String targetEntry = "xl/sharedStrings.xml";
        
        try (InputStream is = Files.newInputStream(inFs.getPath(targetEntry));
                OutputStream os = Files.newOutputStream(outFs.getPath(targetEntry),
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            
            XMLEventReader reader = inFactory.createXMLEventReader(is, "UTF-8");
            XMLEventWriter writer = outFactory.createXMLEventWriter(os, "UTF-8");
            
            reader = FilteringReader.builder(reader)
                    .addFilter(QNAME.COLOR)
                    .build();
            
            writer.add(reader);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(targetEntry + " エントリの処理に失敗しました。", e);
        }
    }
    
    /**
     * 2-2. xl/styles.xml エントリに対する処理を行います。<br>
     * 具体的には、当該エントリ内の色に関わる各種要素を除去することにより、
     * セル背景色、罫線色、フォント色などをクリアします。<br>
     * 
     * @param inFs コピー元Excelブックに対する {@link FileSystem}
     * @param outFs コピー先Excelブックに対する {@link FileSystem}
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    private void processStylesEntry(FileSystem inFs, FileSystem outFs)
            throws ExcelHandlingException {
        
        final String targetEntry = "xl/styles.xml";
        
        try (InputStream is = Files.newInputStream(inFs.getPath(targetEntry));
                OutputStream os = Files.newOutputStream(outFs.getPath(targetEntry),
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            
            XMLEventReader reader = inFactory.createXMLEventReader(is, "UTF-8");
            XMLEventWriter writer = outFactory.createXMLEventWriter(os, "UTF-8");
            
            reader = FilteringReader.builder(reader)
                    .addFilter(QNAME.FONTS, QNAME.FONT, QNAME.COLOR)
                    .addFilter(QNAME.FILLS, QNAME.FILL, QNAME.PATTERN_FILL, QNAME.FG_COLOR)
                    .addFilter(QNAME.FILLS, QNAME.FILL, QNAME.PATTERN_FILL, QNAME.BG_COLOR)
                    .addFilter(QNAME.FILLS, QNAME.FILL, QNAME.GRADIENT_FILL)
                    .addFilter(QNAME.BORDERS, QNAME.BORDER, QNAME.TOP, QNAME.COLOR)
                    .addFilter(QNAME.BORDERS, QNAME.BORDER, QNAME.BOTTOM, QNAME.COLOR)
                    .addFilter(QNAME.BORDERS, QNAME.BORDER, QNAME.LEFT, QNAME.COLOR)
                    .addFilter(QNAME.BORDERS, QNAME.BORDER, QNAME.RIGHT, QNAME.COLOR)
                    .addFilter(QNAME.BORDERS, QNAME.BORDER, QNAME.DIAGONAL, QNAME.COLOR)
                    .addFilter(start -> QNAME.PATTERN_FILL.equals(start.getName())
                            && Optional.ofNullable(start.getAttributeByName(NONS_QNAME.PATTERN_TYPE))
                                    .map(Attribute::getValue)
                                    .map("solid"::equals)
                                    .orElse(false))
                    .build();
            
            writer.add(reader);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(targetEntry + " エントリの処理に失敗しました。", e);
        }
    }
    
    /**
     * 2-3. xl/worksheets/sheet?.xml エントリと xl/styles.xml エントリに対する処理を行います。<br>
     * 具体的には、比較を行った xl/worksheets/sheet?.xml エントリ内の色に関わる各種要素を
     * 除去するとともに、差分個所に色を付けます。<br>
     * 
     * @param inFs
     * @param outFs
     * @param bookPath
     * @param results
     * @throws ApplicationException
     */
    private <T> void processWorksheetEntries(
            FileSystem inFs,
            FileSystem outFs,
            Path bookPath,
            Map<String, Piece<T>> diffs)
            throws ExcelHandlingException {
        
        final String stylesEntry = "xl/styles.xml";
        
        // まず、Excelブック内で共通の xl/styles.xml エントリを読み込む。
        StylesManager stylesManager;
        Document styles;
        
        try (// 注意：コピー先から読み込むため、outFsを使うので正しい。
                InputStream is = Files.newInputStream(outFs.getPath(stylesEntry))) {
            
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            styles = docBuilder.parse(is);
            stylesManager = StylesManager.of(styles);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    stylesEntry + " エントリの読み込みに失敗しました。", e);
        }
        
        // 次に、比較対象シートに対応する xl/worksheets/sheet?.xml エントリに対する着色処理を行う。
        Map<String, String> sheetNameToSource = SaxUtil.loadSheetInfo(bookPath).stream()
                .collect(Collectors.toMap(SheetInfo::name, SheetInfo::source));
        
        for (Entry<String, Piece<T>> diff : diffs.entrySet()) {
            String sheetName = diff.getKey();
            String source = sheetNameToSource.get(sheetName);
            Piece<T> piece = diff.getValue();
            
            processWorksheetEntry(inFs, outFs, stylesManager, source, piece);
        }
        
        // 最後に、xl/styles.xml エントリを上書き保存する。
        try (OutputStream os = Files.newOutputStream(outFs.getPath(stylesEntry),
                StandardOpenOption.TRUNCATE_EXISTING)) {
            
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(styles);
            StreamResult result = new StreamResult(os);
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new ExcelHandlingException(stylesEntry + " エントリの保存に失敗しました。", e);
        }
    }
    
    private <T> void processWorksheetEntry(
            FileSystem inFs,
            FileSystem outFs,
            StylesManager stylesManager,
            String source,
            Piece<T> piece)
            throws ExcelHandlingException {
        
        try (InputStream is = Files.newInputStream(inFs.getPath(source));
                OutputStream os = Files.newOutputStream(outFs.getPath(source),
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            
            XMLEventReader reader = inFactory.createXMLEventReader(is, "UTF-8");
            XMLEventWriter writer = outFactory.createXMLEventWriter(os, "UTF-8");
            
            // リーダーを何重にも重ねるのは処理効率的にイマイチであり
            // ひとつのリーダーにまとめたいという思いもあるものの、
            // ここは人間様にとっての分かりやすさを優先し、
            // 個別のリーダーとして実装することにした。
            
            // 不要な要素を除去するリーダーを追加
            reader = FilteringReader.builder(reader)
                    .addFilter(QNAME.SHEET_PR, QNAME.TAB_COLOR)
                    .addFilter(QNAME.CONDITIONAL_FORMATTING)
                    .build();
            
            // 余剰列にデフォルト色を付けるリーダーを追加
            reader = PaintColumnsReader.of(
                    reader,
                    stylesManager,
                    piece.redundantColumns(),
                    redundantColor);
            
            // 余剰行にデフォルト色を付けるリーダーを追加
            reader = PaintRowsReader.of(
                    reader,
                    stylesManager,
                    piece.redundantRows(),
                    redundantColor);
            
            // 余剰行や余剰列の上にあるセルに色を付けるリーダーを追加
            reader = PaintRedundantCellsReader.of(
                    reader,
                    stylesManager,
                    piece.redundantRows(),
                    piece.redundantColumns(),
                    redundantColor);
            
            // 差分セルに色を付けるリーダーを追加
            reader = PaintDiffCellsReader.of(
                    reader,
                    stylesManager,
                    piece.diffCells(),
                    diffColor);
            
            writer.add(reader);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(source + " エントリの処理に失敗しました。", e);
        }
    }
}
