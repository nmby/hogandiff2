package xyz.hotchpotch.hogandiff;

import java.util.Objects;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理をJavaFXベースのGUIで実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @param <T> 利用するファクトリの型
 * @author nmby
 */
public class GuiTask<T> extends Task<Void> {
    
    // [static members] ********************************************************
    
    /**
     * 新しいタスクを生成して返します。<br>
     * 
     * @param <T> ファクトリの型
     * @param settings 設定
     * @param factory ファクトリ
     * @return 新しいタスク
     * @throws NullPointerException {@code settings}, {@code factory} のいずれかが {@code null} の場合
     */
    public static <T> Task<Void> of(
            Settings settings,
            Factory<T> factory) {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(factory, "factory");
        
        return new GuiTask<>(settings, factory);
    }
    
    // [instance members] ******************************************************
    
    private final Settings settings;
    private final Factory<T> factory;
    
    private GuiTask(
            Settings settings,
            Factory<T> factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
    }
    
    // FIXME: [No.91 内部実装改善] CUIモード実現時の実装がやっつけ過ぎるので根本的に改善する
    /*package*/ void updateProgress2(long workDone, long max) {
        updateProgress(workDone, max);
    }
    
    // FIXME: [No.91 内部実装改善] CUIモード実現時の実装がやっつけ過ぎるので根本的に改善する
    /*package*/ void updateMessage2(String message) {
        updateMessage(message);
    }
    
    @Override
    protected Void call() throws Exception {
        AppTask.of(settings, factory, this).call();
        
        return null;
    }
}
