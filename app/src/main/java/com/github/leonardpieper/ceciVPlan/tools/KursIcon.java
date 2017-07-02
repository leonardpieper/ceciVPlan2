package com.github.leonardpieper.ceciVPlan.tools;


import com.github.leonardpieper.ceciVPlan.R;

public class KursIcon {

    /**
     * Gibt das zum Kurs gehörige Icon aus
     *
     * @param name Der Kursname zu dem das Icon gehören soll
     * @return Gibt eine RessourceID zurück, unter des das Icon gefunden werden kann
     */
    public static int getResourceIdByName(String name) {
        String arr[] = name.split(" ", 2);
        String fach = arr[0];
        fach = fach.toLowerCase();
        switch (fach) {
            case "bi":
                return R.drawable.ic_biologie_bug;
            case "ch":
                return R.drawable.ic_chemie_poppet;
            case "d":
                return R.drawable.ic_deutsch;
            case "e":
                return R.drawable.ic_englisch;
            case "ek":
                return R.drawable.ic_erdkunde_landscape;
            case "el":
                return R.drawable.ic_ernahrungslehre_dining;
            case "ew":
                return R.drawable.ic_erziehungswissenschaften_child;
            case "f":
                return R.drawable.ic_franzosisch;
            case "ge":
                return R.drawable.ic_geschichte_buste;
            case "if":
                return R.drawable.ic_informatik_computer;
            case "ku":
                return R.drawable.ic_kunst_art;
            case "m":
                return R.drawable.ic_mathe_calc;
            case "mu":
                return R.drawable.ic_musik_note;
            case "pl":
                return R.drawable.ic_philosophie_scroll;
            case "ph":
                return R.drawable.ic_physik_lightbulb;
            case "sw":
                return R.drawable.ic_sozialwissenschaften_group;
            case "s":
                return R.drawable.ic_spanisch;
            case "sp":
                return R.drawable.ic_sport_run;
            case "rk":
                return R.drawable.ic_rechtskunde;
            case "er":
            case "kr":
                return R.drawable.ic_religion;
            default:
                return R.drawable.ic_school_black_24dp;
        }
    }
}
