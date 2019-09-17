package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.StringUtil;

/**
 * Excepブック同士の比較結果を表す不変クラスです。<br>
 * 
 * @param <T> セルデータの型
 * @author nmby
 */
public class BResult<T> {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * Excelブック同士の比較結果を生成して返します。<br>
     * 
     * @param <T> セルデータの型
     * @param sheetPairs 比較したシート名のペア（片側だけの欠損ペアも含む）
     * @param results Excelシート同士の比較結果（片側だけの欠損ペアは含まない）
     * @return Excelブック同士の比較結果
     * @throws NullPointerException
     *          {@code sheetPairs}, {@code results} のいずれかが {@code null} の場合
     */
    public static <T> BResult<T> of(
            List<Pair<String>> sheetPairs,
            Map<Pair<String>, SResult<T>> results) {
        
        Objects.requireNonNull(sheetPairs, "sheetPairs");
        Objects.requireNonNull(results, "results");
        
        return new BResult<>(sheetPairs, results);
    }
    
    // [instance members] ******************************************************
    
    private final List<Pair<String>> sheetPairs;
    private final Map<Pair<String>, SResult<T>> results;
    
    private BResult(
            List<Pair<String>> sheetPairs,
            Map<Pair<String>, SResult<T>> results) {
        
        assert sheetPairs != null;
        assert results != null;
        
        this.sheetPairs = sheetPairs;
        this.results = results;
    }
    
    /**
     * 片側のExcelブックについての差分内容を返します。<br>
     * 
     * @param side Excelブックの側
     * @return 片側のExcelブックについての差分内容（シート名とそのシート上の差分個所のマップ）
     */
    public Map<String, Piece<T>> getPiece(Side side) {
        Objects.requireNonNull(side, "side");
        
        return results.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().get(side),
                        entry -> entry.getValue().getPiece(side)));
    }
    
    private String getText(Function<SResult<T>, String> func) {
        
        return sheetPairs.stream().map(pair -> {
            
            StringBuilder str = new StringBuilder();
            str.append(String.format("  - %s vs %s",
                    pair.isPresentA() ? "[" + pair.a() + "]" : "(なし)",
                    pair.isPresentB() ? "[" + pair.b() + "]" : "(なし)"))
                    .append(BR);
            if (pair.isPaired()) {
                str.append(StringUtil.addPrefix("        ", func.apply(results.get(pair))));
                str.append(BR);
            }
            return str;
            
        }).collect(Collectors.joining(BR));
    }
    
    /**
     * 比較結果のサマリを返します。<br>
     * 
     * @return 比較結果のサマリ
     */
    public String getSummary() {
        return getText(SResult::getSummary);
    }
    
    /**
     * 比較結果の詳細を返します。<br>
     * 
     * @return 比較結果の詳細
     */
    public String getDetail() {
        return getText(SResult::getDetail);
    }
    
    @Override
    public String toString() {
        return getDetail();
    }
}
