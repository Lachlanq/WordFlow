package com.wordflow.app.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wordflow.app.MainActivity;
import com.wordflow.app.R;
import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.databinding.FragmentProfileBinding;

/**
 * 个人中心 Fragment — 用户信息、功能菜单和设置
 *
 * ===== 页面结构 =====
 * - 用户卡片：头像 + 昵称 + 学习天数统计
 * - 菜单列表：6 个功能入口，动态生成
 * - 底部按钮：「重置应用数据」（红色文字）
 *
 * ===== 六个菜单功能 =====
 * 0. 学习设置 → 设置每日学习目标（5-100 词）
 * 1. 进度管理 → 暂停学习（1/3/7天）/ 重置词书进度
 * 2. 词书管理 → 切换到词书 Tab
 * 3. 手动录入 → 添加自定义单词
 * 4. 学习数据 → 弹窗显示详细统计
 * 5. 关于我们 → 版本信息
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private WordFlowApplication app;

    private static final String[][] MENU_ITEMS = {
        {"学习设置", "设置每日学习目标"},
        {"进度管理", "休息一天或重置学习进度"},
        {"词书管理", "管理你的词书"},
        {"手动录入", "添加自定义单词"},
        {"学习数据", "查看学习统计"},
        {"关于我们", "WordFlow v1.0.0"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (WordFlowApplication) requireActivity().getApplication();
        setupMenuItems();
        loadUserInfo();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void setupMenuItems() {
        binding.llMenuContainer.removeAllViews();
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            View row = getLayoutInflater().inflate(R.layout.item_menu_row, binding.llMenuContainer, false);
            ((TextView) row.findViewById(R.id.tv_menu_text)).setText(MENU_ITEMS[i][0]);
            final int index = i;
            row.setOnClickListener(v -> onMenuItemClick(index));
            binding.llMenuContainer.addView(row);
        }
    }

    private void onMenuItemClick(int index) {
        switch (index) {
            case 0: showDailyGoalDialog(); break;
            case 1: showProgressDialog(); break;
            case 2: showWordBookManagement(); break;
            case 3: showAddWordDialog(); break;
            case 4: showLearningStats(); break;
            case 5: showAboutDialog(); break;
        }
    }

    // === 0. 学习设置 ===
    private void showDailyGoalDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("每日学习单词数");
        input.setText(String.valueOf(app.getPreferencesManager().getDailyGoal()));
        input.setPadding(48, 32, 48, 16);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("学习设置")
            .setMessage("设置每日学习目标")
            .setView(input)
            .setPositiveButton("保存", (d, w) -> {
                String text = input.getText().toString();
                int goal = Integer.parseInt(text);
                if (goal >= 5 && goal <= 100) {
                    app.getPreferencesManager().setDailyGoal(goal);
                    Toast.makeText(requireContext(), "每日目标已设为 " + goal + " 词", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "请输入 5-100 之间的数字", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // === 1. 进度管理（休息一天 / 重置进度）===
    private void showProgressDialog() {
        int usedCount = app.getPreferencesManager().getRestDayCountLast30Days();
        String title = "进度管理（近30天已使用 " + usedCount + "/3 次休息）";

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(new String[]{"休息一天（今日不学新词）", "重置学习进度"}, (d, which) -> {
                if (which == 0) {
                    showRestDayConfirmDialog();
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("重置学习进度")
                        .setMessage("确定要重置当前词书的学习进度吗？此操作不可撤销。")
                        .setPositiveButton("确认", (dlg, w) -> {
                            app.getRepository().resetProgressByBook(app.getPreferencesManager().getCurrentBookId());
                            Toast.makeText(requireContext(), "当前词书进度已重置", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            })
            .show();
    }

    private void showRestDayConfirmDialog() {
        if (!app.getPreferencesManager().canUseRestDay()) {
            if (app.getPreferencesManager().isRestDay()) {
                Toast.makeText(requireContext(), "今日已在休息中", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "近30天内休息次数已用完（3/3）", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 计算休息日需要复习的单词数
        long bookId = app.getPreferencesManager().getCurrentBookId();
        new Thread(() -> {
            long now = System.currentTimeMillis();
            int dueCount = app.getDatabase().learningRecordDao().getDueReviewCountByBook(now, bookId);
            int reviewLimit = Math.max(1, (int) Math.ceil(dueCount * 0.1));

            requireActivity().runOnUiThread(() ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("休息一天")
                    .setMessage("确定今天休息吗？\n\n休息期间无需学习新单词，只需复习 " + reviewLimit + " 个单词即可。\n\n连续打卡不会中断哦～")
                    .setPositiveButton("确定休息", (dlg, w) -> {
                        app.getPreferencesManager().useRestDay();
                        Toast.makeText(requireContext(), "已开启休息模式，今天只需复习 " + reviewLimit + " 个单词", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show());
        }).start();
    }

    // === 2. 词书管理 ===
    private void showWordBookManagement() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).switchToTab(R.id.nav_books);
        }
    }

    // === 3. 手动录入 ===
    private void showAddWordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_word, null);
        TextView etEnglish = dialogView.findViewById(R.id.et_english);
        TextView etChinese = dialogView.findViewById(R.id.et_chinese);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView).setCancelable(true);
        final android.app.Dialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String english = etEnglish.getText().toString().trim();
            String chinese = etChinese.getText().toString().trim();
            if (!english.isEmpty() && !chinese.isEmpty()) {
                app.getRepository().insertCustomWord(english, chinese);
                Toast.makeText(requireContext(), "已添加「" + english + "」", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    // === 4. 学习数据 ===
    private void showLearningStats() {
        long bookId = app.getPreferencesManager().getCurrentBookId();

        // Build stats message by observing LiveData (simplified: use synchronous DAO calls on bg thread)
        new Thread(() -> {
            int totalDays = app.getDatabase().dailyStatsDao().getTotalDays();
            int mastered = app.getDatabase().learningRecordDao().getMasteredCountByBook(bookId);
            int learning = app.getDatabase().learningRecordDao().getLearningCountByBook(bookId);
            int due = app.getDatabase().learningRecordDao().getDueReviewCountByBook(System.currentTimeMillis(), bookId);
            long todayStart = getTodayStart();
            int todayLearned = app.getDatabase().learningRecordDao().getTodayLearnedCountByBook(todayStart, bookId);

            String msg = "📅 累计学习天数：" + totalDays + " 天\n" +
                         "✅ 已掌握词汇：" + mastered + " 个\n" +
                         "📖 学习中词汇：" + learning + " 个\n" +
                         "⏰ 待复习词汇：" + due + " 个\n" +
                         "📝 今日已学：" + todayLearned + " 词";

            requireActivity().runOnUiThread(() ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("学习数据统计")
                    .setMessage(msg)
                    .setPositiveButton("关闭", null)
                    .show());
        }).start();
    }

    private long getTodayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // === 5. 关于我们 ===
    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于 WordFlow")
            .setMessage("WordFlow v1.0.0\n\n一款基于艾宾浩斯遗忘曲线的智能单词记忆工具。\n\n采用 SM-2 间隔记忆算法，帮助您科学高效地背单词。\n\n让学习更简单，让记忆更持久。")
            .setPositiveButton("知道了", null)
            .show();
    }

    private void loadUserInfo() {
        long bookId = app.getPreferencesManager().getCurrentBookId();
        binding.tvNickname.setText(app.getPreferencesManager().getNickname());

        app.getRepository().getTotalDays().observe(getViewLifecycleOwner(), totalDays -> {
            if (totalDays == null) return;
            app.getRepository().getTodayLearnedCount(bookId).observe(getViewLifecycleOwner(), today -> {
                if (today != null) {
                    binding.tvLearnDays.setText("已坚持学习 " + totalDays + " 天 | 今日已学 " + today + " 词");
                }
            });
        });

        // 加载连续打卡天数
        app.getRepository().getConsecutiveDays().observe(getViewLifecycleOwner(), streak -> {
            if (streak != null) {
                binding.tvStreakDays.setText(streak + " 天");
            }
        });
    }

    private void setupClickListeners() {
        binding.tvLogout.setText("重置应用数据");
        binding.tvLogout.setOnClickListener(v ->
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("重置应用数据")
                .setMessage("确定要重置所有学习数据和设置吗？此操作不可撤销。")
                .setPositiveButton("确认", (d, w) -> {
                    app.getRepository().resetAllProgress();
                    app.getPreferencesManager().clearAll();
                    Toast.makeText(requireContext(), "数据已重置", Toast.LENGTH_SHORT).show();
                    loadUserInfo();
                })
                .setNegativeButton("取消", null)
                .show());

        binding.cardUser.setOnClickListener(v -> showEditNicknameDialog());
    }

    private void showEditNicknameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_word, null);
        TextView etEnglish = dialogView.findViewById(R.id.et_english);
        TextView etChinese = dialogView.findViewById(R.id.et_chinese);
        TextView tvTitle = dialogView.findViewById(R.id.tv_add_word_title);

        if (tvTitle != null) tvTitle.setText("修改昵称");
        etEnglish.setHint("输入新昵称");
        View chineseParent = (View) etChinese.getParent();
        if (chineseParent != null) chineseParent.setVisibility(View.GONE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView).setCancelable(true);
        final android.app.Dialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String nickname = etEnglish.getText().toString().trim();
            if (!nickname.isEmpty()) {
                app.getPreferencesManager().setNickname(nickname);
                binding.tvNickname.setText(nickname);
                Toast.makeText(requireContext(), "昵称已更新", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
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
