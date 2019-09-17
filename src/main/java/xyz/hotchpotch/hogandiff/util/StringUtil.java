package xyz.hotchpotch.hogandiff.util;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文字列操作に関するユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class StringUtil {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * 与えられた文字列の各行の先頭に指定されたプレフィックスを付与した
     * 新しい文字列を返します。<br>
     * 
     * @param prefix 各行の先頭に付与するプレフィックス
     * @param original 元の文字列
     * @return 新しい文字列
     * @throws NullPointerException
     *          {@code prefix}, {@code original} のいずれかが {@code null} の場合
     */
    public static String addPrefix(String prefix, String original) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(original, "original");
        
        return original.lines()
                .map(str -> prefix + str)
                .collect(Collectors.joining(BR));
    }
    
    // [instance members] ******************************************************
    
    private StringUtil() {
    }
}
