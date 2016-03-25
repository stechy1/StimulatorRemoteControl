package cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.manager;


import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.Scheme;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.handler.IReadWriteScheme;
import cz.zcu.fav.tymsnu.stimulatorremotecontrol.model.handler.file.SchemeFileJSONHandler;

public final class SchemeManager extends Observable {

    // region Variables
    private static final String TAG = "SchemeManager";

    // Reference na pracovní adresár
    private File workingDirectory;
    // Přiznak označující, zda-li byl pracoví adresář proskenován
    private boolean loaded = false;

    // Kolekce všech schémat
    private final List<Scheme> schemeList = new ArrayList<>();
    // Reference na aktuálně zvolené schéma
    private Scheme selectedScheme;
    // endregion


    // region Constructors
    /**
     * Konstruktor scheme manageru
     */
    public SchemeManager() {}
    // endregion

    // region Private methods

    /**
     * Načte všechny schémata do paměti
     * Načte pouze jejich názvy
     */
    private void loadSchemes() {
        if (loaded)
            return;

        loaded = true;
        if (workingDirectory == null)
            return;

        schemeList.clear();

        File[] schemes = workingDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().contains(".json");
            }
        });

        for (File file : schemes) {
            String name = file.getName();
            schemeList.add(new Scheme(name));
        }
    }

    /**
     * Načte celé schéma ze souboru
     * @param scheme Reference na schema, které má být načteno
     */
    private void loadScheme(Scheme scheme) {
        if (scheme.loaded)
            return;

        try {
            File file = new File(workingDirectory, scheme.getName() + ".json");
            InputStream in = new FileInputStream(file);
            new SchemeFileJSONHandler().read(in, scheme);
        } catch (IOException e) {
            e.printStackTrace();
        }

        scheme.loaded = true;
        Log.i(TAG, "Scheme: " + scheme + " is loaded");
    }
    // endregion

    // region Public methods

    /**
     * Vytvoří nové schéma
     * @param name Název schématu
     */
    public void create(String name) {create(name, null);}
    /**
     * Vytvoří nové schéma
     * @param name Název schématu
     * @param callback Callback který se zavolá po úspěšném vytvoření schématu
     */
    public void create(String name, Callback callback) {
        Scheme scheme = new Scheme(name);
        scheme.loaded = true;
        schemeList.add(scheme);
        save(scheme);

        if (callback != null)
            callback.callack(scheme);
    }

    public void add(Scheme scheme) {add(scheme, null);}
    public void add(Scheme scheme, Callback callback) {


        if (callback != null)
            callback.callack(scheme);
    }

    /**
     * Uloží schéma do souboru
     * @param scheme Referene na schéma
     */
    public void save(Scheme scheme) {save(scheme, null);}
    /**
     * Uloží schéma do souboru
     * @param scheme Referene na schéma
     * @param callback Callback který se zavolá po úspěšném uložení schématu
     */
    public void save(Scheme scheme, Callback callback) {

        try {
            String name = scheme.getName();
            if (!name.contains(".json"))
                name += ".json";

            File outFile = new File(workingDirectory, name);
            outFile.createNewFile();
            FileOutputStream out = new FileOutputStream(outFile);
            IReadWriteScheme readWrite = new SchemeFileJSONHandler();
            readWrite.write(out, scheme);

            if (callback != null)
                callback.callack(scheme);
        } catch (IOException e) {
            Log.e(TAG, "Nepodarilo se zapsat do souboru");
        }
    }

    /**
     * Uloží všechna načtená schémata
     */
    public void saveAll() {saveAll(null);}
    /**
     * Uloží všechna načtená schémata
     * @param callbackAfterAll Callback, který se zavolá po uložení všech schémat
     */
    public void saveAll(Callback callbackAfterAll) {
        saveAll(callbackAfterAll, null);
    }
    /**
     * Uloží všechna načtená schémata
     * @param callbackAfterAll Callback, který se zavolá po uložení všech schémat
     * @param callbackAfterOne Callback, který se zavolá po uložení jednoho schématu
     */
    public void saveAll(Callback callbackAfterAll, Callback callbackAfterOne) {
        int counter = 0;
        for (Scheme scheme : schemeList) {
            if (scheme.loaded) {
                save(scheme);
                counter++;
                if (callbackAfterOne != null)
                    callbackAfterOne.callack(scheme);
            }
        }

        if (callbackAfterAll != null)
            callbackAfterAll.callack(counter);
    }

    /**
     * Smaže schéma
     * @param scheme Reference na schéma
     */
    public void delete(Scheme scheme) {delete(scheme, null);}
    /**
     * Smaže schéma
     * @param scheme Reference na schéma
     * @param callback Callback který se zavolá po úspěšném smazání schématu
     */
    public void delete(Scheme scheme, Callback callback) {
        Log.i(TAG, "Mazu schema: " + scheme);

        String name = scheme.getName();
        if (!name.contains(".json"))
            name += ".json";

        File file = new File(workingDirectory, name);
        if (!file.exists() || file.isDirectory() || !file.isFile()) {
            schemeList.remove(scheme);
            if (scheme.equals(selectedScheme)) {
                selectedScheme = null;
                notifyValueChanged();
            }
            return;
        }

        if (file.delete()) {
            schemeList.remove(scheme);
            if (scheme.equals(selectedScheme)) {
                selectedScheme = null;
                notifyValueChanged();
            }
            if (callback != null)
                callback.callack(scheme);
        }
    }

    /**
     * Smaže všechna schémata
     */
    public void deleteAll() {
        Iterator<Scheme> it = schemeList.iterator();
        for (Scheme scheme = it.next(); it.hasNext();) {
            delete(scheme);
            it.remove();
        }
    }

    /**
     * Označí schéma jako vybrané
     * @param actScheme Reference na schéma
     */
    public void select(Scheme actScheme) {select(actScheme, null);}
    /**
     * Označí schéma jako vybrané
     * @param actScheme Reference na schéma
     * @param callback Callback který se zavolá po úspěšné změně schématu
     */
    public void select(Scheme actScheme, Callback callback) {
        if (selectedScheme != null && actScheme.equals(selectedScheme))
            return;

        if (this.selectedScheme != null) {
            this.selectedScheme.selected = false;
        }

        actScheme.selected = true;
        this.selectedScheme = actScheme;
        if (!actScheme.loaded)
            loadScheme(actScheme);

        if (callback != null)
            callback.callack(actScheme);

        notifyValueChanged();
    }

    /**
     * Oznámí všechny přihlášené pozorovatele, že se změníly hodnoty ve vybraném schématu
     */
    public void notifyValueChanged() {
        setChanged();
        notifyObservers(selectedScheme);
    }
    // endregion

    // region Getters & Setters
    public Scheme getSelectedScheme() {
        return selectedScheme;
    }

    public List<Scheme> getSchemeList() {
        return schemeList;
    }

    /**
     * Nastaví nový pracovní adresář
     * Nesmí se jednat o soubor
     * @param workingDirectory Reference na složku s pracovním adresářem
     */
    public void setWorkingDirectory(File workingDirectory) {
        if (workingDirectory.isFile() || !workingDirectory.isDirectory())
            return;

        this.workingDirectory = workingDirectory;
        loaded = false;
        loadSchemes();
    }

    // endregion

    /**
     * Rozhraní pro zpětné volání
     */
    public interface Callback {
        void callack(Object object);
    }

    /**
     * Značkovací rozhraní pro klasický observer
     */
    public interface OnSchemeChangeListener extends Observer {}
}
