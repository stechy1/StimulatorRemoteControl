package cz.zcu.fav.tymsnu.stimulatorremotecontrol.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import cz.zcu.fav.tymsnu.stimulatorremotecontrol.R;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.adapter.SimpleConfigurationAdapter;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.bytes.Packet;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.AConfiguration;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.CorruptedConfigurationException;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.manager.Manager;

public class SimpleConfigurationFragment<T extends AConfiguration<T>> extends ASimpleScreen<T>
        implements AdapterView.OnItemClickListener, Observer {

    private static final String TAG = "SimpleConfigFragment";

    private ListView listView;
    private boolean canDismiss;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_universal_screen_1, container, false);

        listView = (ListView) v.findViewById(R.id.universal_screen_1_container_listview);
        listView.setAdapter(buildAdapter());
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);

        ImageButton btnNewConfiguration = (ImageButton) v.findViewById(R.id.universal_screen_1_btn_new_configuration);
        btnNewConfiguration.setOnClickListener(new NewSchemeListener());

        ImageButton btnSaveAll = (ImageButton) v.findViewById(R.id.universal_screen_1_btn_save_all);
        btnSaveAll.setOnClickListener(new SaveAllSchemesListener());

        ImageButton buttonPlay = (ImageButton) v.findViewById(R.id.universal_screen_1_btn_play);
        buttonPlay.setOnClickListener(new PlayConfigurationListener());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        manager.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        manager.deleteObserver(this);
    }

    // ListView onItemClick
    @Override
    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        T selected = (T) listView.getItemAtPosition(position);
        manager.select(selected, new Manager.Callback() {
            @Override
            public void callback(Object object) {
                /*ImageView img = (ImageView) view.findViewById(R.id.control_list_view_image);
                img.setImageResource(R.drawable.checkbox_marked_outline);*/
                ((SimpleConfigurationAdapter) listView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    // ListView onCreateContextMenu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.context_menu_crud, menu);
        menu.setHeaderTitle(R.string.context_options);
    }

    // ListView onContextItemSelected
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        final int listPosition = info.position;
        final T configuration = manager.itemList.get(listPosition);

        switch (item.getItemId()) {
            case R.id.context_duplicate:
                showInputDialog(new DialogCallback() {
                    @Override
                    public void callback(String res) {
                        try {
                            T duplicated = manager.duplicate(configuration, res);
                            manager.add(duplicated);
                            manager.notifyValueChanged();
                            canDismiss = true;
                        } catch (IllegalArgumentException ex) {
                            Toast.makeText(getContext(), R.string.illegal_input, Toast.LENGTH_SHORT).show();
                            canDismiss = false;
                        } catch (CorruptedConfigurationException ex) {
                            Toast.makeText(getContext(), R.string.exception_corrupted_configuration, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            case R.id.context_delete:
                manager.delete(configuration, new Manager.Callback() {
                    @Override
                    public void callback(Object object) {
                        Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.configuration_was_deleted), Snackbar.LENGTH_SHORT).show();
                        listView.requestLayout();
                    }
                });
                return true;
            case R.id.context_rename:
                showInputDialog(new DialogCallback() {
                    @Override
                    public void callback(String newName) {
                        try {
                            manager.rename(configuration, newName);
                            Log.i(TAG, "Nazev schematu: " + newName);
                            canDismiss = true;
                        } catch (IllegalArgumentException ex) {
                            Toast.makeText(getContext(), R.string.illegal_input, Toast.LENGTH_SHORT).show();
                            canDismiss = false;
                        }
                    }
                });
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private ArrayAdapter<T> buildAdapter() {
        return new SimpleConfigurationAdapter<>(getContext(), manager.itemList);
    }

    // Při aktualizaci datasetu v manageru (Změna schématu, změna nastavení výstupů...)
    @Override
    public void update(Observable observable, Object object) {
        Log.i(TAG, "Data update");
        ((SimpleConfigurationAdapter) listView.getAdapter()).notifyDataSetChanged();
    }

    private void showInputDialog(final DialogCallback callback) {
        canDismiss = true;
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(getString(R.string.hint_config_name));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.context_set_name);
        builder.setView(input);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = input.getText().toString();
                callback.callback(name);

                if (canDismiss)
                    dialog.dismiss();
            }
        });

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    private final class NewSchemeListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            showInputDialog(new DialogCallback() {
                @Override
                public void callback(String newName) {
                    try {
                        manager.create(newName, new Manager.Callback() {
                            @Override
                            public void callback(Object object) {
                                ((SimpleConfigurationAdapter) listView.getAdapter()).notifyDataSetChanged();
                            }
                        });
                        Log.i(TAG, "Nazev schematu: " + newName);
                        canDismiss = true;
                    } catch (IllegalArgumentException ex) {
                        Toast.makeText(getContext(), R.string.illegal_input, Toast.LENGTH_SHORT).show();
                        canDismiss = false;
                    }
                }
            });
        }
    }

    private final class SaveAllSchemesListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            manager.saveAll(new Manager.Callback() {
                @Override
                public void callback(Object object) {
                    Integer count = (Integer) object;
                    Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.count_saved_configuration, count), Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    private final class PlayConfigurationListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            T configuration = manager.getSelectedItem();
            if (configuration == null)
                Snackbar.make(getActivity().findViewById(android.R.id.content), "Vyberte konfiguraci pro spuštění experimentu", Snackbar.LENGTH_LONG).show();
            else {
                Snackbar.make(getActivity().findViewById(android.R.id.content), "Spouštím experiment...", Snackbar.LENGTH_LONG).show();
                List<Packet> packets = configuration.getPackets();
                for (Packet packet : packets) {
                    Log.i(TAG, packet.toString());
                    if (!iBtCommunication.write(packet.getValue())) {
                        break;
                    }
                }
            }
        }
    }

    private interface DialogCallback {
        void callback(String res);
    }
}
