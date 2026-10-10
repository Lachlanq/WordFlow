package com.wordflow.app.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.data.model.DailyStats;
import com.wordflow.app.databinding.FragmentAnalyticsBinding;
import com.wordflow.app.util.EbbinghausAlgorithm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

import kotlin.Pair;

/**
 * 数据分析 Fragment — 以可视化图表展示学习数据
 *
 * ===== 页面组成 =====
 * - 四个统计卡片：累计学习天数 / 已掌握词汇 / 今日学习量 / 复习率
 * - 近 7 天学习量柱状图（BarChart）— MPAndroidChart 库绘制
 * - 遗忘曲线折线图（LineChart）— 艾宾浩斯理论遗忘数据
 *
 * ===== MPAndroidChart =====
 * Android 最流行的开源图表库（GitHub: PhilJay/MPAndroidChart）。
 * CUBIC_BEZIER 模式让折线更平滑。PercentFormatter 将 Y 轴格式化为百分比。
 */
public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private WordFlowApplication app;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (WordFlowApplication) requireActivity().getApplication();
        loadStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        long bookId = app.getPreferencesManager().getCurrentBookId();

        app.getRepository().getTotalDays().observe(getViewLifecycleOwner(), totalDays -> {
            if (totalDays != null) binding.tvDays.setText(String.valueOf(totalDays));
        });

        app.getRepository().getMasteredCount(bookId).observe(getViewLifecycleOwner(), mastered -> {
            if (mastered != null) binding.tvMastered.setText(String.valueOf(mastered));
        });

        app.getRepository().getTodayLearnedCount(bookId).observe(getViewLifecycleOwner(), today -> {
            if (today != null) {
                binding.tvTodayCount.setText(String.valueOf(today));
                int totalGoal = app.getPreferencesManager().getDailyGoal();
                int percent = totalGoal > 0 ? Math.min((today * 100) / totalGoal, 100) : 0;
                binding.tvReviewRate.setText(percent + "%");
            }
        });

        setupWeeklyChart();
        setupForgettingCurve();
    }

    private void setupWeeklyChart() {
        app.getRepository().getLast7DaysStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;

            List<String> dayLabels = getLast7DayLabels();
            Map<String, String> dateToLabel = getLast7DaysDateMap();

            // Map date -> newWordsLearned
            Map<String, Integer> statsByDate = new LinkedHashMap<>();
            for (DailyStats s : stats) statsByDate.put(s.getDate(), s.getNewWordsLearned());

            List<BarEntry> entries = new ArrayList<>();
            int index = 0;
            for (String label : dayLabels) {
                String dateKey = null;
                for (Map.Entry<String, String> e : dateToLabel.entrySet()) {
                    if (e.getValue().equals(label)) { dateKey = e.getKey(); break; }
                }
                Integer count = dateKey != null ? statsByDate.getOrDefault(dateKey, 0) : 0;
                entries.add(new BarEntry(index++, count != null ? count.floatValue() : 0f));
            }

            BarDataSet dataSet = new BarDataSet(entries, "学习量");
            dataSet.setColor(Color.parseColor("#4CAF50"));
            dataSet.setValueTextSize(10f);

            binding.chartWeekly.setData(new BarData(dataSet));
            binding.chartWeekly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dayLabels));
            binding.chartWeekly.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            binding.chartWeekly.getXAxis().setGranularity(1f);
            binding.chartWeekly.getXAxis().setDrawGridLines(false);
            binding.chartWeekly.getAxisLeft().setAxisMinimum(0f);
            binding.chartWeekly.getAxisLeft().setGranularity(5f);
            binding.chartWeekly.getAxisRight().setEnabled(false);
            binding.chartWeekly.getDescription().setEnabled(false);
            binding.chartWeekly.getLegend().setEnabled(false);
            binding.chartWeekly.animateY(500);
            binding.chartWeekly.invalidate();
        });
    }

    private List<String> getLast7DayLabels() {
        Calendar cal = Calendar.getInstance();
        List<String> labels = new ArrayList<>();
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
            labels.add(dayNames[dow]);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return labels;
    }

    private Map<String, String> getLast7DaysDateMap() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Map<String, String> map = new LinkedHashMap<>();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(cal.getTime());
            int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
            map.put(dateKey, dayNames[dow]);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return map;
    }

    private void setupForgettingCurve() {
        List<Pair<Float, Float>> curveData = EbbinghausAlgorithm.INSTANCE.getForgettingCurveData();
        List<Entry> entries = new ArrayList<>();
        for (Pair<Float, Float> p : curveData) {
            entries.add(new Entry(p.getFirst(), p.getSecond()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "记忆保留率");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setValueTextSize(10f);
        dataSet.setCircleColor(Color.parseColor("#FF9800"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        binding.chartForgetting.setData(new LineData(dataSet));
        binding.chartForgetting.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.chartForgetting.getXAxis().setDrawGridLines(false);
        binding.chartForgetting.getAxisLeft().setAxisMinimum(0f);
        binding.chartForgetting.getAxisLeft().setAxisMaximum(1.2f);
        binding.chartForgetting.getAxisLeft().setValueFormatter(new PercentFormatter());
        binding.chartForgetting.getAxisRight().setEnabled(false);
        binding.chartForgetting.getDescription().setEnabled(false);
        binding.chartForgetting.getLegend().setEnabled(false);
        binding.chartForgetting.animateX(500);
        binding.chartForgetting.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
