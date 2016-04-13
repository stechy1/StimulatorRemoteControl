package cz.zcu.fav.tymsnu.stimulatorremotecontrol.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cz.zcu.fav.tymsnu.stimulatorremotecontrol.R;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.adapter.HelpAdapter;

public class HelpFragment extends ASimpleFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_help, container, false);

        ExpandableListView expandableListView = (ExpandableListView) v.findViewById(R.id.help_view);
        expandableListView.setAdapter(new HelpAdapter(getContext(),getParentItems(), getChildren()));

        return v;
    }

    private List<String> getParentItems() {
        List<String> parents = new ArrayList<>(3);
        parents.addAll(Arrays.asList(getResources().getStringArray(R.array.help_parents)));

        return parents;
    }

    private HashMap<String, List<String>> getChildren() {
        HashMap<String, List<String>> childrens = new HashMap<>(2);

        childrens.put("ERP", Arrays.asList(getResources().getStringArray(R.array.help_erp_childs)));
        childrens.put("BCI", Arrays.asList("Sub4", "Sub5", "Sub6"));
        childrens.put("Reaction experiment", Arrays.asList("Sub7", "Sub8", "Sub9"));

        return childrens;
    }
}
