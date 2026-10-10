package com.wordflow.app.detail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.databinding.ActivityWordDetailBinding;

/**
 * 单词详情页 — 展示单个单词的完整信息
 *
 * ===== 页面组成 =====
 * - 顶部工具栏：返回箭头 + 标题
 * - 单词区：英文单词（大字体）+ 英式音标 + 美式音标
 * - 释义区：主要释义 + 辅助释义
 * - 搭配区：常见短语搭配
 * - 近义词区：近义词列表
 * - 例句区：英文例句 + 中文翻译
 *
 * ===== Intent 传参 =====
 * 从首页/学习页点击单词 → Intent 携带 word_id → 本页接收并查询数据库显示详情
 */
public class WordDetailActivity extends AppCompatActivity {

    private ActivityWordDetailBinding binding;
    private WordFlowApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWordDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        app = (WordFlowApplication) getApplication();

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long wordId = getIntent().getLongExtra("word_id", -1);
        if (wordId != -1) loadWord(wordId);

        binding.btnBackLearn.setOnClickListener(v -> finish());
    }

    private void loadWord(long wordId) {
        app.getRepository().getWordById(wordId).observe(this, w -> {
            if (w == null) return;
            binding.tvWord.setText(w.getWord());
            binding.tvPhoneticUk.setText("英 " + w.getPhoneticUk());
            binding.tvPhoneticUs.setText("美 " + w.getPhoneticUs());
            binding.tvMeaning.setText(w.getMainMeaning() +
                (w.getSubMeaning().isEmpty() ? "" : "\n" + w.getSubMeaning()));
            binding.tvCollocation.setText(w.getCollocation().isEmpty() ? "暂无" : w.getCollocation());
            binding.tvSynonym.setText(w.getSynonym().isEmpty() ? "暂无" : w.getSynonym());
            binding.tvExample.setText(w.getExampleEn().isEmpty() ?
                "暂无例句" : w.getExampleEn() + "\n" + w.getExampleCn());
        });
    }
}