package macq.intercor.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;

import macq.intercor.R;
import macq.intercor.adapters.ImageAdapter;
import macq.intercor.models.Icon;

public class IconsActionsFragment extends Fragment {

    private static final String TAG = IconsActionsFragment.class.getSimpleName();
    private Handler[] handlers;
    private Runnable[] runnables;
    private OnDataPass dataPasser;
    private View view;
    private SharedPreferences sharedPreferences;

    // This method will be invoked when the Fragment view object is created.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.view = inflater.inflate(R.layout.icons_actions_fragment, container, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return this.view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.dataPasser = (OnDataPass) context;
    }

    public void createGrid(ArrayList<Icon> icons) {
        GridView grid = this.view.findViewById(R.id.icons);
        grid.setAdapter(new ImageAdapter(getContext(), icons));
        this.handlers = new Handler[grid.getCount()];
        this.runnables = new Runnable[grid.getCount()];
        for(int i = 0; i < grid.getCount(); i++) {
            this.handlers[i] = new Handler();
        }
        grid.setOnItemClickListener(getClickListener());
    }

    public boolean isGridCreated() {
        GridView grid = this.view.findViewById(R.id.icons);
        if(grid.getAdapter() == null) return false;
        return grid.getAdapter().areAllItemsEnabled();
    }

    @NonNull
    private AdapterView.OnItemClickListener getClickListener() {
        return (parent, view, position, id) -> {
            Icon iconClicked = (Icon)parent.getItemAtPosition(position);
            iconClicked.setSelected(!iconClicked.getSelected());
            if (iconClicked.getSelected()) { // Select item
                view.setBackground(view.getContext().getDrawable(R.drawable.border_pending));
                this.runnables[position] = createRunnable(view, iconClicked);
                this.dataPasser.onIconPass(iconClicked);
                int gracePeriod = Integer.parseInt(sharedPreferences.getString("time_span", "5"));
                this.handlers[position].postDelayed(this.runnables[position], gracePeriod * 1000 * 60);
            } else { // deselection
                view.setBackground(view.getContext().getDrawable(R.drawable.border));
                this.handlers[position].removeCallbacks(this.runnables[position]); // remove the post-delayed cb
                this.handlers[position].post(this.runnables[position]); // use directly the runnable
                this.runnables[position] = null;
            }
        };
    }

    private Runnable createRunnable(final View view, final Icon iconClicked) {
        return () -> {
            view.setBackground(view.getContext().getDrawable(R.drawable.border_sent));
            this.dataPasser.onIconPass(iconClicked);
            Handler h = new Handler();
            h.postDelayed(() -> {
                view.setBackground(view.getContext().getDrawable(R.drawable.border));
                iconClicked.setSelected(false);
            }, 1000);
        };
    }

    public interface OnDataPass {
        void onIconPass(Icon icon);
    }
}
