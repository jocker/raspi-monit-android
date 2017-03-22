package rpi.aut.rpi_monit.components.rv;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.ViewHolder> {


    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final BaseAdapter mAdapter;
        public ViewHolder(View itemView, BaseAdapter adapter) {
            super(itemView);
            mAdapter = adapter;
        }
    }
}
