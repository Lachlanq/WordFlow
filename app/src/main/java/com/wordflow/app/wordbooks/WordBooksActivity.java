package com.wordflow.app.wordbooks;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 词书 Activity — 占位页面
 *
 * ===== 通俗理解 =====
 * 这个 Activity 只是一个"空壳"，创建后立刻关闭（finish）。
 * 实际的词书界面是通过 WordBooksFragment 嵌入在 MainActivity 中展示的。
 *
 * ===== 为什么存在？ =====
 * 导航系统（nav_graph.xml）中注册了这个 Activity 作为独立目的地。
 * 当某些代码路径直接启动 WordBooksActivity 时，这里做个空实现防止崩溃。
 * 正常流程中用户永远不会看到这个页面。
 */
public class WordBooksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();  // 立即关闭，用户无感知
    }
}
