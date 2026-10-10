package com.wordflow.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.wordflow.app.databinding.ActivityMainBinding;

/**
 * 主页面（MainActivity）— App 的核心容器
 *
 * ===== 通俗理解 =====
 * MainActivity 本身不包含业务 UI，它只是一个"容器"：
 *   上半部分：NavHostFragment — 用来装 4 个 Fragment 页面（首页/词书/数据/我的）
 *   下半部分：BottomNavigationView — 底部 4 个 Tab 图标按钮
 *
 * ===== Navigation Component 导航机制 =====
 * Android Jetpack 提供的页面导航框架，原理：
 *   1. NavHostFragment：是一个"Fragment 容器"，负责显示和切换 Fragment
 *   2. NavController：导航控制器，管理 Fragment 之间的跳转、动画和返回栈
 *   3. setupWithNavController：一行代码绑定底部导航栏和页面切换
 *      → 点击 Tab → 自动切换到对应的 Fragment
 *      → 滑动页面 → 底部 Tab 自动高亮
 *
 * ===== switchToTab() 方法 =====
 * 供其他页面调用，通过代码切换底部导航标签。
 * 例如：个人中心 → switchToTab(R.id.nav_books) → 跳到词书 Tab。
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 找到 Fragment 容器（NavHostFragment）
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment);

        // 检查休息日状态是否跨天需清除
        ((WordFlowApplication) getApplication()).getPreferencesManager().clearRestDayIfNewDay();

        // 将底部导航栏与导航控制器绑定（一行代码完成 Tab ↔ Fragment 联动）
        NavigationUI.setupWithNavController(binding.bottomNav, navHost.getNavController());
    }

    /**
     * 切换到指定 Tab
     * @param tabId 目标 Tab 的 ID，如 R.id.nav_home（首页）、R.id.nav_books（词书）
     */
    public void switchToTab(int tabId) {
        binding.bottomNav.setSelectedItemId(tabId);
    }
}
