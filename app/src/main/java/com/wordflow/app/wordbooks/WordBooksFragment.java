package com.wordflow.app.wordbooks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wordflow.app.R;
import com.wordflow.app.WordFlowApplication;
import com.wordflow.app.data.model.WordBook;
import com.wordflow.app.databinding.FragmentWordBooksBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 词书列表 Fragment — 展示所有词书，支持切换当前词书
 *
 * ===== LiveData 响应式更新 =====
 * 使用 observe 监听词书数据的 LiveData。当数据库中的词书信息变化时
 * （例如切换了当前词书，isCurrent 字段变化），LiveData 自动推送新数据，
 * Adapter 自动刷新列表，无需手动调用刷新。
 */
public class WordBooksFragment extends Fragment {

    private FragmentWordBooksBinding binding;
    private WordFlowApplication app;
    private WordBookAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWordBooksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (WordFlowApplication) requireActivity().getApplication();

        adapter = new WordBookAdapter(book -> {
            app.getRepository().setCurrentWordBook(book.getId());
            app.getPreferencesManager().setCurrentBookId(book.getId());
            Toast.makeText(requireContext(), "已切换至：" + book.getName(), Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
        });

        binding.rvWordBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWordBooks.setAdapter(adapter);

        app.getRepository().getAllWordBooks().observe(getViewLifecycleOwner(), books -> {
            if (books != null) adapter.submitList(books);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

class WordBookAdapter extends RecyclerView.Adapter<WordBookAdapter.ViewHolder> {

    interface OnBookClickListener { void onBookClick(WordBook book); }

    private final OnBookClickListener listener;
    private List<WordBook> books = new ArrayList<>();

    WordBookAdapter(OnBookClickListener listener) { this.listener = listener; }

    void submitList(List<WordBook> list) { books = list; notifyDataSetChanged(); }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word_book, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordBook book = books.get(position);
        holder.tvBookName.setText(book.getName());
        holder.tvBookCount.setText(book.getWordCount() + " 个单词");
        holder.tvStatus.setVisibility(book.isCurrent() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onBookClick(book));
    }

    @Override public int getItemCount() { return books.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookName, tvBookCount, tvStatus;
        ViewHolder(View v) {
            super(v);
            tvBookName = v.findViewById(R.id.tv_book_name);
            tvBookCount = v.findViewById(R.id.tv_book_count);
            tvStatus = v.findViewById(R.id.tv_status);
        }
    }
}
