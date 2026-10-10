package com.wordflow.app.learn;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wordflow.app.R;
import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.data.model.Word;
import com.wordflow.app.databinding.ActivityLearnBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习/复习页 — App 最核心的交互页面
 *
 * ===== 闪卡学习法（Flashcard Method） =====
 * 1. 看到卡片正面（英文单词）→ 尝试回忆中文意思
 * 2. 点击卡片"翻转"→ 揭晓答案（中文释义 + 例句）
 * 3. 自我评估记忆情况 → 选择「认识」「模糊」「不认识」
 * 4. SM-2 算法自动计算下一次复习时间
 *
 * 这种"主动回忆"比"被动阅读"效果好得多，因为回忆本身就是记忆强化的过程。
 *
 * ===== 两个模式 =====
 * MODE_LEARN（学习模式）：学习新单词。进度如 3/20。
 * MODE_REVIEW（复习模式）：复习到达复习时间的单词。进度如 1/5。
 */
public class LearnActivity extends AppCompatActivity {

    /** 模式常量：学习新单词 */
    public static final String MODE_LEARN = "learn";
    /** 模式常量：复习旧单词 */
    public static final String MODE_REVIEW = "review";

    private ActivityLearnBinding binding;
    private WordFlowApplication app;
    private List<Word> words = new ArrayList<>();
    private int currentIndex = 0;
    private int dailyGoal = 20;
    private int learnedBefore = 0;
    private long currentBookId = 1;
    private boolean isRevealed = false;
    private String mode = MODE_LEARN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLearnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        app = (WordFlowApplication) getApplication();

        binding.btnPronounce.setVisibility(View.GONE);
        binding.btnBookmark.setVisibility(View.GONE);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = MODE_LEARN;
        dailyGoal = app.getPreferencesManager().getDailyGoal();
        currentBookId = app.getPreferencesManager().getCurrentBookId();

        setupClickListeners();
        loadWords();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnKnow.setOnClickListener(v -> onFeedback(2));
        binding.btnVague.setOnClickListener(v -> onFeedback(1));
        binding.btnUnknown.setOnClickListener(v -> onFeedback(0));
        binding.cardWord.setOnClickListener(v -> {
            if (!isRevealed && currentIndex < words.size()) revealMeaning();
        });
    }

    private void loadWords() {
        currentBookId = app.getPreferencesManager().getCurrentBookId();
        boolean isRestDay = app.getPreferencesManager().isRestDay();

        if (MODE_REVIEW.equals(mode)) {
            if (isRestDay) {
                // 休息日：只复习 10% 的待复习单词（至少 1 个）
                app.getRepository().getRestDayReviewWords(currentBookId).observe(this, list -> {
                    if (list == null) return;
                    words = list;
                    learnedBefore = 0;
                    if (words.isEmpty()) {
                        binding.tvWord.setText("🎉");
                        binding.tvMainMeaning.setText("今日没有需要复习的单词，好好休息吧！");
                        binding.tvPhonetic.setVisibility(View.GONE);
                        binding.tvSubMeaning.setVisibility(View.GONE);
                        binding.tvExample.setVisibility(View.GONE);
                        binding.llFeedback.setVisibility(View.GONE);
                        binding.tvProgressText.setText("休息日");
                        binding.pbLearn.setMax(1);
                        binding.pbLearn.setProgress(1);
                    } else {
                        currentIndex = 0;
                        showWord(0);
                    }
                });
            } else {
                app.getRepository().getWordsForReview(currentBookId).observe(this, list -> {
                    if (list == null) return;
                    words = list;
                    learnedBefore = 0;
                    if (words.isEmpty()) showEmptyState();
                    else { currentIndex = 0; showWord(0); }
                });
            }
        } else {
            // MODE_LEARN
            if (isRestDay) {
                // 休息日不学新词，直接显示完成状态
                binding.tvWord.setText("☕");
                binding.tvMainMeaning.setText("今日休息，无需学习新词");
                binding.tvPhonetic.setVisibility(View.GONE);
                binding.tvSubMeaning.setVisibility(View.GONE);
                binding.tvExample.setVisibility(View.GONE);
                binding.llFeedback.setVisibility(View.GONE);
                binding.tvProgressText.setText("休息日");
                binding.pbLearn.setMax(1);
                binding.pbLearn.setProgress(1);
            } else {
                app.getRepository().getTodayLearnedCount(currentBookId).observe(this, learned -> {
                    if (learned == null) return;
                    learnedBefore = learned;
                    app.getRepository().getWordsForLearning(currentBookId, dailyGoal).observe(this, list -> {
                        if (list == null) return;
                        words = list;
                        if (words.isEmpty()) showEmptyState();
                        else { currentIndex = 0; showWord(0); }
                    });
                });
            }
        }
    }

    private void showEmptyState() {
        binding.tvWord.setText("🎉");
        binding.tvMainMeaning.setText(MODE_REVIEW.equals(mode) ?
            "没有需要复习的单词！" : "今日单词已学完，休息一下吧！");
        binding.tvPhonetic.setVisibility(View.GONE);
        binding.tvSubMeaning.setVisibility(View.GONE);
        binding.tvExample.setVisibility(View.GONE);
        binding.llFeedback.setVisibility(View.GONE);

        if (MODE_LEARN.equals(mode)) {
            binding.tvProgressText.setText(dailyGoal + "/" + dailyGoal);
            binding.pbLearn.setMax(Math.max(dailyGoal, 1));
            binding.pbLearn.setProgress(dailyGoal);
            checkAndPromptReview();
        } else {
            binding.tvProgressText.setText("已完成");
            binding.pbLearn.setMax(1);
            binding.pbLearn.setProgress(1);
        }
    }

    private void showWord(int index) {
        if (index >= words.size()) {
            binding.tvWord.setText("🎉");
            binding.tvMainMeaning.setText("全部完成！太棒了！");
            binding.tvPhonetic.setVisibility(View.GONE);
            binding.tvSubMeaning.setVisibility(View.GONE);
            binding.tvExample.setVisibility(View.GONE);
            binding.llFeedback.setVisibility(View.GONE);

            if (MODE_LEARN.equals(mode)) {
                binding.tvProgressText.setText(dailyGoal + "/" + dailyGoal);
                binding.pbLearn.setMax(Math.max(dailyGoal, 1));
                binding.pbLearn.setProgress(dailyGoal);
                checkAndPromptReview();
            } else {
                binding.tvProgressText.setText(words.size() + "/" + words.size());
                binding.pbLearn.setMax(Math.max(words.size(), 1));
                binding.pbLearn.setProgress(words.size());
            }
            return;
        }

        Word w = words.get(index);
        isRevealed = false;

        binding.tvWord.setText(w.getWord());
        binding.tvPhonetic.setVisibility(View.VISIBLE);
        binding.tvPhonetic.setText(w.getPhoneticUk().isEmpty() ? "" : w.getPhoneticUk());
        binding.tvMainMeaning.setText("点击卡片显示释义");
        binding.tvMainMeaning.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        binding.tvSubMeaning.setVisibility(View.GONE);
        binding.tvExample.setVisibility(View.GONE);
        binding.llFeedback.setVisibility(View.VISIBLE);

        if (MODE_LEARN.equals(mode)) {
            int pos = learnedBefore + index + 1;
            binding.tvProgressText.setText(pos + "/" + dailyGoal);
            binding.pbLearn.setMax(Math.max(dailyGoal, 1));
            binding.pbLearn.setProgress(Math.min(pos, dailyGoal));
        } else {
            binding.tvProgressText.setText((index + 1) + "/" + words.size());
            binding.pbLearn.setMax(Math.max(words.size(), 1));
            binding.pbLearn.setProgress(index + 1);
        }
    }

    private void revealMeaning() {
        if (currentIndex >= words.size()) return;
        isRevealed = true;

        Word w = words.get(currentIndex);
        binding.tvMainMeaning.setText(w.getMainMeaning());
        binding.tvMainMeaning.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        if (!w.getSubMeaning().isEmpty()) {
            binding.tvSubMeaning.setVisibility(View.VISIBLE);
            binding.tvSubMeaning.setText(w.getSubMeaning());
        }
        if (!w.getExampleEn().isEmpty()) {
            binding.tvExample.setVisibility(View.VISIBLE);
            binding.tvExample.setText(w.getExampleEn() + "\n" + w.getExampleCn());
        }
    }

    private void onFeedback(int familiarity) {
        if (currentIndex >= words.size()) return;
        if (!isRevealed) {
            revealMeaning();
            return;
        }
        app.getRepository().recordLearning(words.get(currentIndex).getId(), familiarity);
        currentIndex++;
        showWord(currentIndex);
    }

    private void checkAndPromptReview() {
        app.getRepository().getDueReviewCount(currentBookId).observe(this, dueCount -> {
            if (dueCount != null && dueCount > 0) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("学习完成！")
                    .setMessage("恭喜完成今日学习目标！\n\n还有 " + dueCount + " 个单词可以复习，是否立即开始？")
                    .setPositiveButton("开始复习", (d, w) -> switchToReviewMode())
                    .setNegativeButton("稍后再说", null)
                    .show();
            }
        });
    }

    private void switchToReviewMode() {
        mode = MODE_REVIEW;
        currentIndex = 0;
        isRevealed = false;
        loadWords();
    }
}