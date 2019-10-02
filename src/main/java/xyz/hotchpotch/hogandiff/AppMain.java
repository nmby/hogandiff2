package xyz.hotchpotch.hogandiff;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import xyz.hotchpotch.hogandiff.excel.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.feature.basic.BasicFactory;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * このアプリケーションのエントリポイントです。<br>
 *
 * @author nmby
 */
public class AppMain extends Application {
    
    // [static members] ********************************************************
    
    /** このアプリケーションのバージョン */
    private static final String VERSION = "v0.5.1";
    
    /** プロパティファイルの相対パス */
    private static final Path APP_PROP_PATH = Path.of("hogandiff.properties");
    
    /** プロパティファイルに記録すべき設定項目 */
    // FIXME: [No.91 内部実装改善] settings周りの実装が不細工すぎるので改善する
    public static final Set<Settings.Key<?>> keysToBeSaved = Set.of(
            SettingKeys.CONSIDER_ROW_GAPS,
            SettingKeys.CONSIDER_COLUMN_GAPS,
            SettingKeys.COMPARE_ON_FORMULA_STRING,
            AppSettingKeys.SHOW_PAINTED_SHEETS,
            AppSettingKeys.SHOW_RESULT_TEXT,
            AppSettingKeys.EXIT_WHEN_FINISHED);
    
    private static Settings settings;
    
    /**
     * このアプリケーションのエントリポイントです。<br>
     * 
     * @param args アプリケーション実行時引数
     */
    public static void main(String[] args) {
        settings = arrangeSettings(args);
        if (settings.containsKey(AppSettingKeys.CUI_MODE) && settings.get(AppSettingKeys.CUI_MODE)) {
            // FIXME: [No.91 内部実装改善] CUIモード実現時の実装がやっつけ過ぎるので根本的に改善する
            try {
                settings = Settings.builder(settings)
                        .setDefaultValue(AppSettingKeys.CURR_TIMESTAMP)
                        .setDefaultValue(AppSettingKeys.WORK_DIR_BASE)
                        .build();
                AppTask.of(settings, BasicFactory.of(), null).call();
                Platform.exit();
            } catch (ApplicationException e) {
                e.printStackTrace();
            }
        } else {
            launch(args);
        }
    }
    
    /**
     * プロパティファイルを読み込み、プロパティセットを返します。<br>
     * プロパティファイルが存在しない場合は、空のプロパティセットを返します。<br>
     * 
     * @return プロパティセット
     */
    public static Properties loadProperties() {
        Properties properties = new Properties();
        try (Reader r = Files.newBufferedReader(APP_PROP_PATH)) {
            properties.load(r);
        } catch (Exception e) {
            // nop
        }
        return properties;
    }
    
    /**
     * 指定されたプロパティセットをプロパティファイルに保存します。<br>
     * 
     * @param properties プロパティセット
     * @throws NullPointerException {@code properties} が {@code null} の場合
     */
    public static void storeProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        
        try (Writer w = Files.newBufferedWriter(APP_PROP_PATH)) {
            properties.store(w, null);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(AlertType.ERROR, "設定の保存に失敗しました。", ButtonType.OK)
                    .showAndWait();
        }
    }
    
    // FIXME: [No.91 内部実装改善] settings周りの実装が不細工すぎるので改善する
    private static Settings arrangeSettings(String[] args) {
        assert args != null;
        
        try {
            // 1. プロパティファイルから設定を抽出する。
            Properties properties = loadProperties();
            Settings.Builder builder = Settings.builder(properties, keysToBeSaved);
            
            // 2. アプリケーション実行時引数から設定を抽出する。
            Optional<Settings> fromArgs = AppArgsParser.parseArgs(args);
            if (0 < args.length && fromArgs.isEmpty()) {
                System.err.println(AppArgsParser.USAGE);
            }
            
            // 3. アプリケーション実行時引数から設定を抽出できた場合は、
            //    その内容でプロパティファイルからの内容を上書きする。
            //    つまり、アプリケーション実行時引数で指定された内容を優先させる。
            if (fromArgs.isPresent()) {
                builder.setAll(fromArgs.get());
            }
            
            return builder.build();
            
        } catch (RuntimeException e) {
            // 何らかの実行時例外が発生した場合は空の設定を返すことにする。
            return Settings.builder().build();
        }
    }
    
    // [instance members] ******************************************************
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GuiView.fxml"));
        Parent root = loader.load();
        Image icon = new Image(getClass().getResourceAsStream("favicon.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("方眼Diff  -  " + VERSION);
        primaryStage.setScene(new Scene(root, 500, 430));
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(450);
        
        GuiController controller = loader.getController();
        controller.applySettings(settings);
        
        primaryStage.show();
        
        if (controller.isReady()) {
            controller.execute();
        }
    }
}
