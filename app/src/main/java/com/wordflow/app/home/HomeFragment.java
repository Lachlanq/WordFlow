package com.wordflow.app.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.wordflow.app.MainActivity;
import com.wordflow.app.R;
import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.data.model.Word;
import com.wordflow.app.databinding.FragmentHomeBinding;
import com.wordflow.app.detail.WordDetailActivity;
import com.wordflow.app.learn.LearnActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 Fragment — 学习仪表盘，App 最常使用的页面
 *
 * ===== 页面组成 =====
 * - 圆形进度环：今日学习目标完成度（百分比 + 分数）
 * - 三行关键数据：今日新词 / 待复习 / 已掌握
 * - 四个快捷入口：开始学习 / 今日复习 / 词书选择 / 手动录入
 * - RecyclerView 列表：新词预览（最多 5 个）
 *
 * ===== 数据刷新 =====
 * loadData() 在 onViewCreated 和 onResume 中都会被调用。
 * onResume 确保从学习页返回时数据是最新的。
 * 每个统计项独立 observe LiveData，数据变化时自动更新对应 UI。
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private WordFlowApplication app;
    private WordPreviewAdapter wordAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (WordFlowApplication) requireActivity().getApplication();

        setupRecyclerView();
        setupClickListeners();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupRecyclerView() {
        wordAdapter = new WordPreviewAdapter(word -> {
            Intent intent = new Intent(requireContext(), WordDetailActivity.class);
            intent.putExtra("word_id", word.getId());
            startActivity(intent);
        });
        binding.rvNewWords.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNewWords.setAdapter(wordAdapter);
    }

    private void setupClickListeners() {
        binding.llStartLearn.setOnClickListener(v -> {
            if (app.getPreferencesManager().isRestDay()) {
                return; // 休息日不允许学习新词
            }
            startActivity(new Intent(requireContext(), LearnActivity.class));
        });

        binding.llReview.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LearnActivity.class);
            intent.putExtra("mode", LearnActivity.MODE_REVIEW);
            startActivity(intent);
        });

        binding.llWordBooks.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).switchToTab(R.id.nav_books);
            }
        });

        binding.llManual.setOnClickListener(v -> showAddWordDialog());
    }

    private void loadData() {
        long bookId = app.getPreferencesManager().getCurrentBookId();
        int totalGoal = app.getPreferencesManager().getDailyGoal();

        // 检查休息日状态（跨天自动清除）
        app.getPreferencesManager().clearRestDayIfNewDay();
        boolean isRestDay = app.getPreferencesManager().isRestDay();

        app.getRepository().getMasteredCount(bookId).observe(getViewLifecycleOwner(), mastered -> {
            if (mastered == null) return;
            binding.tvMasteredCount.setText("已掌握: " + mastered);
        });

        app.getRepository().getLearningCount(bookId).observe(getViewLifecycleOwner(), learning -> {
            if (learning == null) return;
            // shown as part of overall stats
        });

        app.getRepository().getDueReviewCount(bookId).observe(getViewLifecycleOwner(), due -> {
            if (due == null) return;
            binding.tvDueReview.setText("待复习: " + due);
        });

        app.getRepository().getTotalDays().observe(getViewLifecycleOwner(), days -> {
            // total days info used elsewhere
        });

        app.getRepository().getTodayLearnedCount(bookId).observe(getViewLifecycleOwner(), today -> {
            if (today == null) return;
            int percent = totalGoal > 0 ? Math.min((today * 100) / totalGoal, 100) : 0;
            binding.progressCircle.setProgress(percent);
            binding.tvProgressPercent.setText(percent + "%");
            binding.tvProgressCount.setText(today + "/" + totalGoal);
            binding.tvNewWordsToday.setText("今日新词: " + today);
        });

        // New words preview
        app.getRepository().getWordsForLearning(bookId, 5).observe(getViewLifecycleOwner(), words -> {
            if (words != null) wordAdapter.submitList(words);
        });

        // 休息日：禁用新词学习
        if (isRestDay) {
            binding.llStartLearn.setAlpha(0.5f);
            binding.llStartLearn.setClickable(false);
            binding.tvNewWordsToday.setText("今日休息，不学新词");
        } else {
            binding.llStartLearn.setAlpha(1.0f);
            binding.llStartLearn.setClickable(true);
        }
    }

    private void showAddWordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_word, null);
        TextInputEditText etEnglish = dialogView.findViewById(R.id.et_english);
        TextInputEditText etChinese = dialogView.findViewById(R.id.et_chinese);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true);
        final android.app.Dialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String english = etEnglish.getText().toString().trim();
            String chinese = etChinese.getText().toString().trim();
            if (!english.isEmpty() && !chinese.isEmpty()) {
                app.getRepository().insertCustomWord(english, chinese);
                dialog.dismiss();
                loadData();
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

/** RecyclerView adapter for word preview list */
class WordPreviewAdapter extends RecyclerView.Adapter<WordPreviewAdapter.ViewHolder> {

    interface OnWordClickListener { void onWordClick(Word word); }

    private final OnWordClickListener listener;
    private List<Word> words = new ArrayList<>();

    WordPreviewAdapter(OnWordClickListener listener) { this.listener = listener; }

    void submitList(List<Word> list) { words = list; notifyDataSetChanged(); }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Word word = words.get(position);
        holder.tvWord.setText(word.getWord());
        holder.tvMeaning.setText(word.getMainMeaning());
        holder.itemView.setOnClickListener(v -> listener.onWordClick(word));
    }

    @Override public int getItemCount() { return words.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord, tvMeaning;
        ViewHolder(View v) {
            super(v);
            tvWord = v.findViewById(R.id.tv_word);
            tvMeaning = v.findViewById(R.id.tv_meaning);
        }
    }
}
