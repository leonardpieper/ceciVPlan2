package com.github.leonardpieper.ceciVPlan;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.models.Kurs;
import com.github.leonardpieper.ceciVPlan.tools.Kurse;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity2 extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                if (preference instanceof EditTextPreference && ((EditTextPreference) preference).getEditText().getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    //Damit Passwörter in der Summary nicht im Klartext angezeigt werden
                    preference.setSummary(((EditTextPreference) preference).getEditText().getTransformationMethod().getTransformation(stringValue, ((EditTextPreference) preference).getEditText()).toString());
                } else {
                    preference.setSummary(stringValue);
                }

                if (preference instanceof EditTextPreference && preference.getKey().equals("pref_vplan_etpref_user")) {
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    if (mAuth.getCurrentUser() != null) {
                        DatabaseReference vplanRef = MyDatabaseUtil.getDatabase().getReference()
                                .child("Users")
                                .child(mAuth.getCurrentUser().getUid())
                                .child("vPlan")
                                .child("uname");
                        vplanRef.setValue(stringValue);
                    }
                } else if (preference instanceof EditTextPreference && preference.getKey().equals("pref_vplan_etpref_pwd")) {
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    if (mAuth.getCurrentUser() != null) {
                        DatabaseReference vplanRef = MyDatabaseUtil.getDatabase().getReference()
                                .child("Users")
                                .child(mAuth.getCurrentUser().getUid())
                                .child("vPlan")
                                .child("pwd");
                        vplanRef.setValue(stringValue);
                    }
                }

            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new MainPreferenceFragment()).commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
//        super.onBuildHeaders(target);
//        addPreferencesFromResource();
//        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || AccountPreferenceFragment.class.getName().equals(fragmentName)
                || VPlanPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            setHasOptionsMenu(true);

//            Preference generalButton = findPreference("pref_main_pref_general");
//            generalButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    getFragmentManager().beginTransaction().replace(android.R.id.content,
//                            new GeneralPreferenceFragment()).commit();
//                    return true;
//                }
//            });
            Preference accountButton = findPreference("pref_main_pref_account");
            accountButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content,
                            new AccountPreferenceFragment()).commit();
                    return true;
                }
            });
            Preference vplanButton = findPreference("pref_main_pref_vplan");
            vplanButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content,
                            new VPlanPreferenceFragment()).commit();
                    return true;
                }
            });
            Preference kurseButton = findPreference("pref_main_pref_kurse");
            kurseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content,
                            new KursPreferenceFragment()).commit();
                    return true;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AccountPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_account);
            setHasOptionsMenu(true);

            final FirebaseAuth mAuth = FirebaseAuth.getInstance();

            if (mAuth.getCurrentUser() != null) {
                Preference etEmail =  findPreference("pref_account_etpref_email");
                Preference etPwd = findPreference("pref_account_etpref_pwd");
                Preference etPhone =  findPreference("pref_account_etpref_phone");
                Preference pSignOut = findPreference("pref_account_pref_signout");

                if(mAuth.getCurrentUser().getEmail()!=null&&!mAuth.getCurrentUser().getEmail().isEmpty()){
                    etEmail.setSummary(mAuth.getCurrentUser().getEmail());
                }else {
                    PreferenceCategory category = (PreferenceCategory)findPreference("pref_account_cat_data");
                    category.removePreference(etEmail);
                    category.removePreference(etPwd);
                }

                if(mAuth.getCurrentUser().getPhoneNumber()!=null&&!mAuth.getCurrentUser().getPhoneNumber().isEmpty()){
                    etPhone.setSummary(mAuth.getCurrentUser().getPhoneNumber());
                }else {
                    PreferenceCategory category = (PreferenceCategory)findPreference("pref_account_cat_data");
                    category.removePreference(etPhone);
                }



                pSignOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        mAuth.signOut();
                        Toast.makeText(getActivity(), "Abgemeldet",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class VPlanPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_vplan);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pref_vplan_etpref_user"));
            bindPreferenceSummaryToValue(findPreference("pref_vplan_etpref_pwd"));
            bindPreferenceSummaryToValue(findPreference("jahrgang"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class KursPreferenceFragment extends PreferenceFragment {
        PreferenceScreen screen;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_vplan);
            setHasOptionsMenu(true);

            screen = getPreferenceManager().createPreferenceScreen(getActivity());
            getKurse();
            setPreferenceScreen(screen);
        }

        private void getKurse() {
            final Kurse kurse = new Kurse(getActivity());
            ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Kurs> kursListe = new ArrayList<>();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        kursListe.add(childSnapshot.getValue(Kurs.class));
                    }
                    java.util.Collections.reverse(kursListe);
                    displayKurse(kursListe);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            List<Kurs> kurses = kurse.getKurse(valueEventListener);
            if (kurses != null) {
                displayKurse(kurses);
            }
        }

        private void displayKurse(List<Kurs> kursListe) {
            for (Kurs kurs : kursListe) {
                final String title = kurs.name;
                String type = kurs.type;

                Preference kursPref = new Preference(getActivity());
                kursPref.setTitle(title);
                kursPref.setSummary(type);
                kursPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayKursAlertDialog(title);
                        return true;
                    }
                });

                screen.addPreference(kursPref);
            }
        }

        private void displayKursAlertDialog(final String name) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final Kurse kurse = new Kurse(getActivity());

            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View textEntryView = factory.inflate(R.layout.dialog_kurse_add, null);


            final EditText kursName = (EditText) textEntryView.findViewById(R.id.dialog_add_abk);
            kursName.setVisibility(View.GONE);
            final EditText kursSecret = (EditText) textEntryView.findViewById(R.id.dialog_add_pwd);
            final CheckBox checkBox = (CheckBox) textEntryView.findViewById(R.id.dialog_kurse_add_chkbx_offline);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(checkBox.isChecked()){
                        kursSecret.setVisibility(View.GONE);
                    }else {
                        kursSecret.setVisibility(View.VISIBLE);
                    }
                }
            });


            builder.setView(textEntryView);
            builder.setTitle(name + " bearbeiten");
            builder.setPositiveButton("Bestätigen", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (checkBox.isChecked()) {
                        kurse.joinKurs(name, kursSecret.getText().toString(), "offline");
                    } else {
                        kurse.joinKurs(name, kursSecret.getText().toString(), "online");
                    }
                }
            });
            builder.setNegativeButton("Löschen", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    kurse.leaveKurs(name);
                }
            });
            AlertDialog dialog = builder.show();
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
