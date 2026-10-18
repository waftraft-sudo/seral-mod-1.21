package com.seral.util;

import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;

public final class BiomeUtils {

    private BiomeUtils() {}

    // クラスのロード時（またはサーバー起動時）に一度だけ正規データを取得してキャッシュする
    // これにより、毎回 OverworldBiomeBuilder を生成するコストをゼロにします
    private static final Climate.Parameter[] TEMP_THRESHOLDS = new OverworldBiomeBuilder().getTemperatureThresholds();
    private static final Climate.Parameter[] HUMIDITY_THRESHOLDS = new OverworldBiomeBuilder().getHumidityThresholds();

    /**
     * ノイズ値から気温レベル(0～4)を判定するメソッド
     * Stringではなく int を返すので高速です
     * * @param rawNoiseValue NoiseRouterから取得した生のdouble値
     */
    public static int getTemperatureIndex(double rawNoiseValue) {
        // バニラと同じ計算式で「量子化(Quantize)」します
        // これを行わないと、境界付近でバニラの判定とズレます
        long quantizedValue = Climate.quantizeCoord((float) rawNoiseValue);

        // キャッシュしておいた配列を使って判定
        // バニラの getDebugString... の中身と同じロジックを最適化して記述
        for (int i = 0; i < TEMP_THRESHOLDS.length; ++i) {
            // .max() は long値を返します
            if (quantizedValue < TEMP_THRESHOLDS[i].max()) {
                return i;
            }
        }
        
        // ここに来るのは想定外ですが、万が一すべての範囲を超えていた場合は一番暑い(4)とみなす
        return 4;
    }

    // 指定した位置の生の気温値を取得するヘルパーメソッド
    public static double getRawTemperature(ServerLevel level, BlockPos pos) {
        RandomState randomState = level.getChunkSource().randomState();

        // 26.3からRandomStateがNoiseRouterを公開しなくなったので、ジェネレーター設定から取得してRandomStateで評価する
        return randomState.sampleBlockValueUncached(getNoiseRouter(level).temperature(), pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * ノイズ値から湿度(植生)レベル(0～4)を判定するメソッド
     * 0: 乾燥 (Dry) -> 4: 多湿 (Wet)
     */
    public static int getVegetationIndex(double rawNoiseValue) {
        // 気温と同じ量子化処理
        long quantizedValue = Climate.quantizeCoord((float) rawNoiseValue);

        for (int i = 0; i < HUMIDITY_THRESHOLDS.length; ++i) {
            if (quantizedValue < HUMIDITY_THRESHOLDS[i].max()) {
                return i;
            }
        }
        return 4;
    }

    // 指定した位置の生のVegetation(Humidity)値を取得するヘルパーメソッド
    public static double getRawVegetation(ServerLevel level, BlockPos pos) {
        RandomState randomState = level.getChunkSource().randomState();

        // NoiseRouterから .vegetation() を取得します
        return randomState.sampleBlockValueUncached(getNoiseRouter(level).vegetation(), pos.getX(), pos.getY(), pos.getZ());
    }

    // ノイズ生成でないワールド（スーパーフラット等）ではバニラと同様に空のNoiseRouterを使う
    private static NoiseRouter getNoiseRouter(ServerLevel level) {
        if (level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noiseGenerator) {
            return noiseGenerator.generatorSettings().value().noiseRouter();
        }
        return NoiseRouterData.none();
    }
}