package com.vijay.jsonwizard.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.shashank.sony.fancydialoglib.Animation;
import com.shashank.sony.fancydialoglib.FancyAlertDialog;
import com.shashank.sony.fancydialoglib.Icon;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.comparisons.Comparison;
import com.vijay.jsonwizard.comparisons.EqualToComparison;
import com.vijay.jsonwizard.comparisons.GreaterThanComparison;
import com.vijay.jsonwizard.comparisons.GreaterThanEqualToComparison;
import com.vijay.jsonwizard.comparisons.LessThanComparison;
import com.vijay.jsonwizard.comparisons.LessThanEqualToComparison;
import com.vijay.jsonwizard.comparisons.NotEqualToComparison;
import com.vijay.jsonwizard.comparisons.RegexComparison;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.customviews.CheckBox;
import com.vijay.jsonwizard.customviews.GenericPopupDialog;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interfaces.GenericPopupInterface;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.interfaces.OnActivityRequestPermissionResultListener;
import com.vijay.jsonwizard.interfaces.OnActivityResultListener;
import com.vijay.jsonwizard.utils.ExObjectResult;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.utils.PropertyManager;
import com.vijay.jsonwizard.utils.SecondaryValueModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS;

public class JsonFormActivity extends AppCompatActivity implements JsonApi, GenericPopupInterface {

    private static final String TAG = JsonFormActivity.class.getSimpleName();
    private GenericPopupDialog genericPopupDialog = GenericPopupDialog.getInstance();
    private FormUtils formUtils = new FormUtils();
    private Toolbar mToolbar;
    private JSONObject mJSONObject;
    private PropertyManager propertyManager;
    private HashMap<String, View> skipLogicViews;
    private HashMap<String, View> constrainedViews;
    private ArrayList<View> formDataViews;
    private String functionRegex;
    private HashMap<String, Comparison> comparisons;
    private HashMap<Integer, OnActivityResultListener> onActivityResultListeners;
    private HashMap<Integer, OnActivityRequestPermissionResultListener> onActivityRequestPermissionResultListeners;
    private String confirmCloseTitle;
    private String confirmCloseMessage;
    private JSONArray extraFieldsWithValues;

    public void init(String json) {
        try {
            mJSONObject = new JSONObject(json);
            if (!mJSONObject.has("encounter_type")) {
                mJSONObject = new JSONObject();
                throw new JSONException("Form encounter_type not set");
            }

            confirmCloseTitle = getString(R.string.confirm_form_close);
            confirmCloseMessage = getString(R.string.confirm_form_close_explanation);

        } catch (JSONException e) {
            Log.d(TAG, "Initialization error. Json passed is invalid : " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.native_form_activity_json_form);
        mToolbar = findViewById(R.id.tb_top);
        setSupportActionBar(mToolbar);
        skipLogicViews = new HashMap<>();
        onActivityResultListeners = new HashMap<>();
        onActivityRequestPermissionResultListeners = new HashMap<>();
        if (savedInstanceState == null) {
            init(getIntent().getStringExtra(JsonFormConstants.JSON_FORM_KEY.JSON));
            initializeFormFragment();
            onFormStart();
        } else {
            init(savedInstanceState.getString("jsonState"));
        }
    }

    public void initializeFormFragment() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, JsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME)).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (onActivityResultListeners.containsKey(requestCode)) {
            onActivityResultListeners.get(requestCode).onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (onActivityRequestPermissionResultListeners.containsKey(requestCode)) {
            onActivityRequestPermissionResultListeners.get(requestCode).onRequestPermissionResult(requestCode, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public synchronized JSONObject getStep(String name) {
        synchronized (mJSONObject) {
            try {
                return mJSONObject.getJSONObject(name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent,
                           String openMrsEntity, String openMrsEntityId, boolean popup) throws JSONException {
        synchronized (mJSONObject) {
            JSONObject jsonObject = mJSONObject.getJSONObject(stepName);
            JSONArray fields = fetchFields(jsonObject, popup);
            for (int i = 0; i < fields.length(); i++) {
                JSONObject item = fields.getJSONObject(i);
                String keyAtIndex = item.getString(JsonFormConstants.KEY);
                String itemType = "";
                if (popup) {
                    itemType = item.getString(JsonFormConstants.TYPE);
                }
                if (key.equals(keyAtIndex)) {
                    if (item.has(JsonFormConstants.TEXT)) {
                        item.put(JsonFormConstants.TEXT, value);
                    } else {
                        if (popup) {
                            String itemText = "";
                            if (itemType.equals(JsonFormConstants.NATIVE_RADIO_BUTTON)) {
                                itemText = getRadioButtonText(item, value);
                            }
                            genericPopupDialog.addSelectedValues(addAssignedValue(keyAtIndex, "", value, itemType, itemText));
                            extraFieldsWithValues = fields;
                        }
                        item.put(JsonFormConstants.VALUE, value);
                    }

                    item.put(JsonFormConstants.OPENMRS_ENTITY_PARENT, openMrsEntityParent);
                    item.put(JsonFormConstants.OPENMRS_ENTITY, openMrsEntity);
                    item.put(JsonFormConstants.OPENMRS_ENTITY_ID, openMrsEntityId);
                    refreshSkipLogic(key, null, popup);
                    refreshConstraints(key, null);
                    refreshMediaLogic(key, value);
                    return;
                }
            }
        }
    }

    private String getRadioButtonText(JSONObject item, String value) {
        String text = "";
        if (item != null && item.has(JsonFormConstants.OPTIONS_FIELD_NAME)) {
            try {
                JSONArray options = item.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.getJSONObject(i);
                    if (option != null && option.has(JsonFormConstants.KEY)) {
                        String key = option.getString(JsonFormConstants.KEY);
                        if (key.equals(value)) {
                            text = option.getString(JsonFormConstants.TEXT);
                        }
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, Log.getStackTraceString(e));
            }

        }
        return text;
    }

    @Override
    public void writeValue(String stepName, String parentKey, String childObjectKey, String childKey,
                           String value, String openMrsEntityParent, String openMrsEntity,
                           String openMrsEntityId, boolean popup)
            throws JSONException {
        synchronized (mJSONObject) {
            JSONObject jsonObject = mJSONObject.getJSONObject(stepName);
            JSONArray fields = fetchFields(jsonObject, popup);
            for (int i = 0; i < fields.length(); i++) {
                JSONObject item = fields.getJSONObject(i);
                String keyAtIndex = item.getString(JsonFormConstants.KEY);
                String itemType = "";
                if (popup) {
                    itemType = item.getString(JsonFormConstants.TYPE);
                }
                if (parentKey.equals(keyAtIndex)) {
                    JSONArray jsonArray = item.getJSONArray(childObjectKey);
                    for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject innerItem = jsonArray.getJSONObject(j);
                        String anotherKeyAtIndex = innerItem.getString(JsonFormConstants.KEY);
                        String itemText = "";
                        if (itemType.equals(JsonFormConstants.CHECK_BOX)) {
                            itemText = innerItem.getString(JsonFormConstants.TEXT);
                        }
                        if (childKey.equals(anotherKeyAtIndex)) {
                            innerItem.put(JsonFormConstants.VALUE, value);
                            if (popup) {
                                genericPopupDialog.addSelectedValues(addAssignedValue(keyAtIndex, childKey, value, itemType, itemText));
                                extraFieldsWithValues = fields;
                            }
                            refreshSkipLogic(parentKey, childKey, popup);
                            refreshConstraints(parentKey, childKey);
                            return;
                        }
                    }
                }
            }
        }
    }

    private Map<String, String> addAssignedValue(String itemKey, String optionKey, String keyValue, String itemType, String itemText) {
        Map<String, String> value = new HashMap<>();
        switch (itemType) {
            case JsonFormConstants.CHECK_BOX:
                value.put(itemKey, optionKey + ":" + itemText + ":" + keyValue + ";" + itemType);
                break;
            case JsonFormConstants.NATIVE_RADIO_BUTTON:
                value.put(itemKey, keyValue + ":" + itemText + ";" + itemType);
                break;
            default:
                value.put(itemKey, keyValue + ";" + itemType);
                break;
        }

        return value;
    }

    @Override
    public void writeMetaDataValue(String metaDataKey, Map<String, String> values) throws JSONException {
        synchronized (mJSONObject) {
            if (mJSONObject.has(FormUtils.METADATA_PROPERTY) && !values.isEmpty() && (mJSONObject.getJSONObject(FormUtils.METADATA_PROPERTY).has(metaDataKey))) {
                JSONObject metaData = mJSONObject.getJSONObject(FormUtils.METADATA_PROPERTY).getJSONObject(metaDataKey);
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value == null) value = "";
                    metaData.put(key, value);
                }
            }

        }
    }

    @Override
    public String currentJsonState() {
        synchronized (mJSONObject) {
            return mJSONObject.toString();
        }
    }

    @Override
    public String getCount() {
        synchronized (mJSONObject) {
            return mJSONObject.optString("count");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("jsonState", mJSONObject.toString());
    }

    @Override
    public void onFormStart() {
        try {
            if (propertyManager == null) {
                propertyManager = new PropertyManager(this);
            }
            FormUtils.updateStartProperties(propertyManager, mJSONObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFormFinish() {
        try {
            if (propertyManager == null) {
                propertyManager = new PropertyManager(this);
            }
            FormUtils.updateEndProperties(propertyManager, mJSONObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearSkipLogicViews() {
        skipLogicViews = new HashMap<>();
    }

    @Override
    public void clearConstrainedViews() {
        constrainedViews = new HashMap<>();
    }

    @Override
    public void clearFormDataViews() {
        formDataViews = new ArrayList<>();
        clearSkipLogicViews();
        clearConstrainedViews();
    }

    @Override
    public void refreshHiddenViews(boolean popup) {
        for (View curView : formDataViews) {
            String addressString = (String) curView.getTag(R.id.address);
            String[] address = addressString.split(":");
            try {
                JSONObject viewData = getObjectUsingAddress(address, popup);
                if (viewData.has("hidden") && viewData.getBoolean("hidden")) {
                    toggleViewVisibility(curView, false, popup);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addSkipLogicView(View view) {
        skipLogicViews.put(getViewKey(view), view);
    }

    @Override
    public void addConstrainedView(View view) {
        constrainedViews.put(getViewKey(view), view);
    }

    private String getViewKey(View view) {
        String key = (String) view.getTag(R.id.key);
        if (view.getTag(R.id.childKey) != null) {
            key = key + ":" + view.getTag(R.id.childKey);
        }

        return key;
    }

    @Override
    public void addFormDataView(View view) {
        formDataViews.add(view);
    }

    @Override
    public ArrayList<View> getFormDataViews() {
        return formDataViews;
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppThemeAlertDialog)
                .setTitle(confirmCloseTitle)
                .setMessage(confirmCloseMessage)
                .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JsonFormActivity.this.finish();
                    }
                }).create();

        dialog.show();
    }

    @Override
    public void refreshSkipLogic(String parentKey, String childKey, boolean popup) {
        initComparisons();
        for (View curView : skipLogicViews.values()) {
            addRelevance(curView, popup);
        }
    }


    private void addRelevance(View view, Boolean popup) {
        String relevanceTag = (String) view.getTag(R.id.relevance);
        if (relevanceTag != null && relevanceTag.length() > 0) {
            try {
                JSONObject relevance = new JSONObject(relevanceTag);
                Iterator<String> keys = relevance.keys();
                boolean ok = true;
                while (keys.hasNext()) {
                    String curKey = keys.next();
                    String[] address = curKey.split(":");
                    if (address.length == 2) {
                        JSONObject curRelevance = relevance.getJSONObject(curKey);
                        Map<String, String> curValueMap = getValueFromAddress(address, popup);

                        try {
                            boolean comparison = isRelevant(curValueMap, curRelevance);
                            ok = ok && comparison;
                            if (!ok) break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (address.length == 3) {
                        Log.i("JSONACT", String.valueOf(address.length));
                    }
                }
                toggleViewVisibility(view, ok, popup);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void toggleViewVisibility(View view, boolean visible, boolean popup) {
        try {
            JSONArray canvasViewIds = new JSONArray((String) view.getTag(R.id.canvas_ids));
            setReadOnlyAndFocus(view, visible, popup);
            for (int i = 0; i < canvasViewIds.length(); i++) {
                int curId = canvasViewIds.getInt(i);
                View curCanvasView = view.getRootView().findViewById(curId);
                if (visible) {
                    if (curCanvasView != null) {
                        curCanvasView.setEnabled(true);
                        curCanvasView.setVisibility(View.VISIBLE);
                    }
                    if (curCanvasView instanceof MaterialEditText || curCanvasView instanceof RelativeLayout || view instanceof LinearLayout) {
                        curCanvasView.setFocusable(true);
                    }
                } else {
                    if (curCanvasView != null) {
                        curCanvasView.setEnabled(false);
                        curCanvasView.setVisibility(View.GONE);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, view.toString());
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void setReadOnlyAndFocus(View view, boolean visible, boolean popup) {
        try {
            String addressString = (String) view.getTag(R.id.address);
            String[] address = addressString.split(":");
            JSONObject object = getObjectUsingAddress(address, popup);

            boolean enabled = visible;
            if (object != null && object.has(JsonFormConstants.READ_ONLY) && object.getBoolean(JsonFormConstants.READ_ONLY) && visible) {
                enabled = false;
            }

            view.setEnabled(enabled);
            if (view instanceof MaterialEditText || view instanceof RelativeLayout || view instanceof LinearLayout) {
                view.setFocusable(enabled);
                if (view instanceof MaterialEditText) {
                    view.setFocusableInTouchMode(enabled);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method checks if all views being watched for constraints enforce those constraints
     * This library currently only supports constraints on views that store the value in {@link MaterialEditText}
     * (ie TreeViews, DatePickers, and EditTexts), and {@link CheckBox}
     */
    @Override
    public void refreshConstraints(String parentKey, String childKey) {
        initComparisons();

        // Priorities constraints on the view that has just been changed
        String changedViewKey = parentKey;
        if (changedViewKey != null && childKey != null) {
            changedViewKey = changedViewKey + ":" + childKey;
        }

        if (changedViewKey != null && constrainedViews.containsKey(changedViewKey)) {
            checkViewConstraints(constrainedViews.get(changedViewKey), false);
        }

        for (View curView : constrainedViews.values()) {
            if (changedViewKey == null || !getViewKey(curView).equals(changedViewKey)) {
                checkViewConstraints(curView, false);
            }
        }
    }

    @Override
    public void addOnActivityResultListener(final Integer requestCode,
                                            OnActivityResultListener onActivityResultListener) {
        onActivityResultListeners.put(requestCode, onActivityResultListener);
    }

    @Override
    public void addOnActivityRequestPermissionResultListener(Integer requestCode, OnActivityRequestPermissionResultListener onActivityRequestPermissionResultListener) {
        onActivityRequestPermissionResultListeners.put(requestCode, onActivityRequestPermissionResultListener);
    }

    @Override
    public void removeOnActivityRequestPermissionResultListener(Integer requestCode) {
        onActivityRequestPermissionResultListeners.remove(requestCode);
    }

    @Override
    public void resetFocus() {
        EditText defaultFocusView = findViewById(R.id.default_focus_view);
        defaultFocusView.requestFocus();
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputManager != null && getCurrentFocus() != null) {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), HIDE_NOT_ALWAYS);
        }
    }

    private void checkViewConstraints(View curView, boolean popup) {
        String constraintTag = (String) curView.getTag(R.id.constraints);
        if (constraintTag != null && constraintTag.length() > 0) {
            try {
                String addressString = (String) curView.getTag(R.id.address);
                String[] address = addressString.split(":");

                JSONArray constraint = new JSONArray(constraintTag);
                String errorMessage = null;
                for (int i = 0; i < constraint.length(); i++) {
                    JSONObject curConstraint = constraint.getJSONObject(i);
                    if (address.length == 2) {
                        String value = getValueFromAddress(address, popup).get(JsonFormConstants.VALUE);
                        errorMessage = enforceConstraint(value, curView, curConstraint);
                        if (errorMessage != null) break;
                    }
                }

                if (errorMessage != null) {
                    if (curView instanceof MaterialEditText) {
                        ((MaterialEditText) curView).setText(null);
                        ((MaterialEditText) curView).setError(errorMessage);
                    } else if (curView instanceof CheckBox) {
                        ((CheckBox) curView).setChecked(false);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        String checkBoxKey = (String) curView.getTag(R.id.childKey);
                        JSONObject questionObject = getObjectUsingAddress(address, popup);
                        for (int i = 0; i < questionObject.getJSONArray("options").length(); i++) {
                            JSONObject curOption = questionObject.getJSONArray("options").getJSONObject(i);
                            if (curOption.getString(JsonFormConstants.KEY).equals(checkBoxKey)) {
                                curOption.put(JsonFormConstants.VALUE, "false");
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> getValueFromAddress(String[] address, boolean popup) throws Exception {
        Map<String, String> result = new HashMap<>();
        JSONObject object = getObjectUsingAddress(address, popup);
        if (object != null) {
            switch (object.getString(JsonFormConstants.TYPE)) {
                case JsonFormConstants.CHECK_BOX:
                    JSONArray options = object.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                    for (int j = 0; j < options.length(); j++) {
                        if (options.getJSONObject(j).has(JsonFormConstants.VALUE)) {
                            result.put(options.getJSONObject(j).getString(JsonFormConstants.KEY), options.getJSONObject(j).getString(JsonFormConstants.VALUE));
                        } else {
                            Log.e(TAG, "option for Key " + options.getJSONObject(j).getString(JsonFormConstants.KEY) + " has NO value");
                        }
                    }

                    break;
                case JsonFormConstants.NATIVE_RADIO_BUTTON:
                    Boolean multiRelevance = object.optBoolean(JsonFormConstants.NATIVE_RADIO_BUTTON_MULTI_RELEVANCE, false);
                    if (multiRelevance) {
                        JSONArray jsonArray = object.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                        for (int j = 0; j < jsonArray.length(); j++) {
                            if (object.has(JsonFormConstants.VALUE)) {
                                if (object.getString(JsonFormConstants.VALUE).equals(jsonArray.getJSONObject(j).getString(JsonFormConstants.KEY))) {
                                    result.put(jsonArray.getJSONObject(j).getString(JsonFormConstants.KEY), String.valueOf(true));
                                } else {
                                    result.put(jsonArray.getJSONObject(j).getString(JsonFormConstants.KEY), String.valueOf(false));
                                }
                            } else {
                                Log.e(TAG, "option for Key " + jsonArray.getJSONObject(j).getString(JsonFormConstants.KEY) + " has NO value");
                            }
                        }
                    } else {
                        result.put(JsonFormConstants.VALUE, object.optString(JsonFormConstants.VALUE));
                    }
                    break;
                default:
                    result.put(JsonFormConstants.VALUE, object.optString(JsonFormConstants.VALUE));
                    break;
            }
        }

        return result;
    }

    @Override
    public JSONObject getObjectUsingAddress(String[] address, boolean popup) throws JSONException {
        if (address != null && address.length == 2) {
            JSONArray fields = fetchFields(mJSONObject.getJSONObject(address[0]), popup);
            for (int i = 0; i < fields.length(); i++) {
                if (fields.getJSONObject(i).getString(JsonFormConstants.KEY).equals(address[1])) {
                    return fields.getJSONObject(i);
                }
            }
        }

        return null;
    }

    private void initComparisons() {
        if (comparisons == null) {
            functionRegex = "";
            comparisons = new HashMap<>();

            LessThanComparison lessThanComparison = new LessThanComparison();
            functionRegex += lessThanComparison.getFunctionName();
            comparisons.put(lessThanComparison.getFunctionName(), lessThanComparison);

            LessThanEqualToComparison lessThanEqualToComparison = new LessThanEqualToComparison();
            functionRegex += "|" + lessThanEqualToComparison.getFunctionName();
            comparisons.put(lessThanEqualToComparison.getFunctionName(), lessThanEqualToComparison);

            EqualToComparison equalToComparison = new EqualToComparison();
            functionRegex += "|" + equalToComparison.getFunctionName();
            comparisons.put(equalToComparison.getFunctionName(), equalToComparison);

            NotEqualToComparison notEqualToComparer = new NotEqualToComparison();
            functionRegex += "|" + notEqualToComparer.getFunctionName();
            comparisons.put(notEqualToComparer.getFunctionName(), notEqualToComparer);

            GreaterThanComparison greaterThanComparison = new GreaterThanComparison();
            functionRegex += "|" + greaterThanComparison.getFunctionName();
            comparisons.put(greaterThanComparison.getFunctionName(), greaterThanComparison);

            GreaterThanEqualToComparison greaterThanEqualToComparison = new GreaterThanEqualToComparison();
            functionRegex += "|" + greaterThanEqualToComparison.getFunctionName();
            comparisons.put(greaterThanEqualToComparison.getFunctionName(), greaterThanEqualToComparison);

            RegexComparison regexComparison = new RegexComparison();
            functionRegex += "|" + regexComparison.getFunctionName();
            comparisons.put(regexComparison.getFunctionName(), regexComparison);
        }
    }

    private boolean doComparison(String value, JSONObject comparison) throws Exception {
        String type = comparison.getString("type").toLowerCase();
        String ex = comparison.getString("ex");

        Pattern pattern = Pattern.compile("(" + functionRegex + ")\\((.*)\\)");
        Matcher matcher = pattern.matcher(ex);
        if (matcher.find()) {
            String functionName = matcher.group(1);
            String b = matcher.group(2);//functions arguments should be two, and should either be addresses or values (enclosed using "")
            String[] args = getFunctionArgs(b, value);
            return comparisons.get(functionName).compare(args[0], type, args[1]);
        }

        return false;
    }

    private String[] getFunctionArgs(String functionArgs, String value) {
        String[] args = new String[2];
        String[] splitArgs = functionArgs.split(",");
        if (splitArgs.length == 2) {
            Pattern valueRegex = Pattern.compile("\"(.*)\"");
            for (int i = 0; i < splitArgs.length; i++) {
                String curArg = splitArgs[i].trim();

                if (".".equals(curArg)) {
                    args[i] = value;
                } else {
                    Matcher valueMatcher = valueRegex.matcher(curArg);
                    if (valueMatcher.find()) {
                        args[i] = valueMatcher.group(1);
                    } else {
                        try {
                            args[i] = getValueFromAddress(curArg.split(":"), false).get(JsonFormConstants.VALUE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return args;
    }

    private void refreshMediaLogic(String key, String value) {
        try {
            JSONObject object = getStep("step1");
            JSONArray fields = object.getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                JSONObject questionGroup = fields.getJSONObject(i);
                if ((questionGroup.has("key") && questionGroup.has("has_media_content"))
                        && (questionGroup.getString("key").equalsIgnoreCase(key))
                        && (questionGroup.getBoolean("has_media_content"))) {
                    JSONArray medias = questionGroup.getJSONArray("media");
                    for (int j = 0; j < medias.length(); j++) {
                        JSONObject media = medias.getJSONObject(j);
                        mediaDialog(media, value);
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    public void mediaDialog(JSONObject media, String value) {
        try {
            if (media.getString("media_trigger_value").equalsIgnoreCase(value)) {
                String mediatype = media.getString("media_type");
                String medialink = media.getString("media_link");
                String mediatext = media.getString("media_text");

                infoDialog(mediatype, medialink, mediatext);
            }
        } catch (Exception e) {

        }
    }

    private void infoDialog(String mediatype, String medialink, String mediatext) {
        final FancyAlertDialog.Builder builder = new FancyAlertDialog.Builder(this);
        builder.setTitle("Info");
        builder.setBackgroundColor(Color.parseColor("#208CC5")).setPositiveBtnBackground(Color.parseColor("#208CC5"))  //Don't pass R.color.colorvalue
                .setPositiveBtnText("OK").setAnimation(Animation.SLIDE)
                .isCancellable(true)
                .setIcon(com.shashank.sony.fancydialoglib.R.drawable.ic_person_black_24dp, Icon.Visible);
        builder.setMessage(mediatext);
        if (mediatype.equalsIgnoreCase("image")) {
            builder.setImagetoshow(medialink);
        } else if (mediatype.equalsIgnoreCase("video")) {
            builder.setVideopath(medialink);
        }
        builder.build();
    }


    /**
     * This method checks whether a constraint has been enforced and returns an error message if not
     * The error message should be displayable to the user
     *
     * @param value      The value to be checked
     * @param constraint The constraint expression to use
     * @return An error message if constraint has not been enfored or NULL if constraint enforced
     * @throws Exception
     */
    private String enforceConstraint(String value, View view, JSONObject constraint) throws Exception {
        String type = constraint.getString("type").toLowerCase();
        String ex = constraint.getString("ex");
        String errorMessage = constraint.getString(JsonFormConstants.ERR);
        Pattern pattern = Pattern.compile("(" + functionRegex + ")\\((.*)\\)");
        Matcher matcher = pattern.matcher(ex);
        if (matcher.find()) {
            String functionName = matcher.group(1);
            String b = matcher.group(2);
            String[] args = getFunctionArgs(b, value);

            boolean viewDoesntHaveValue = TextUtils.isEmpty(value);
            if (view instanceof CheckBox) {
                viewDoesntHaveValue = !((CheckBox) view).isChecked();
            }

            if (viewDoesntHaveValue
                    || TextUtils.isEmpty(args[0])
                    || TextUtils.isEmpty(args[1])
                    || comparisons.get(functionName).compare(args[0], type, args[1])) {
                return null;
            }
        } else {
            Log.d(TAG, "Matcher didn't work with function");
        }

        return errorMessage;
    }

    private JSONArray fetchFields(JSONObject parentJson, Boolean popup) {
        JSONArray fields = new JSONArray();
        try {
            if (parentJson.has(JsonFormConstants.SECTIONS) && parentJson.get(JsonFormConstants.SECTIONS) instanceof JSONArray) {
                JSONArray sections = parentJson.getJSONArray(JsonFormConstants.SECTIONS);

                for (int i = 0; i < sections.length(); i++) {
                    JSONObject sectionJson = sections.getJSONObject(i);
                    if (sectionJson.has(JsonFormConstants.FIELDS)) {
                        if (popup) {
                            JSONArray jsonArray = sectionJson.getJSONArray(JsonFormConstants.FIELDS);
                            for (int k = 0; k < jsonArray.length(); k++) {
                                JSONObject item = jsonArray.getJSONObject(k);
                                if (item.has(JsonFormConstants.EXTRA_REL) && item.has(JsonFormConstants.HAS_EXTRA_REL)) {
                                    fields = formUtils.concatArray(fields, specifyFields(item));
                                }
                            }
                        } else {
                            fields = formUtils.concatArray(fields, sectionJson.getJSONArray(JsonFormConstants.FIELDS));
                        }

                    }
                }
            } else if (parentJson.has(JsonFormConstants.FIELDS) && parentJson.get(JsonFormConstants.FIELDS) instanceof JSONArray) {
                if (popup) {
                    JSONArray jsonArray = parentJson.getJSONArray(JsonFormConstants.FIELDS);
                    for (int k = 0; k < jsonArray.length(); k++) {
                        JSONObject item = jsonArray.getJSONObject(k);
                        if (item.has(JsonFormConstants.EXTRA_REL) && item.has(JsonFormConstants.HAS_EXTRA_REL)) {
                            fields = specifyFields(item);
                        }
                    }
                } else {
                    fields = parentJson.getJSONArray(JsonFormConstants.FIELDS);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fields;
    }

    private JSONArray specifyFields(JSONObject parentJson) {
        JSONArray fields = new JSONArray();
        if (parentJson.has(JsonFormConstants.HAS_EXTRA_REL)) {
            String optionKey;
            try {
                optionKey = (String) parentJson.get(JsonFormConstants.HAS_EXTRA_REL);
                JSONArray options = parentJson.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                if (options.length() > 0) {
                    for (int j = 0; j < options.length(); j++) {
                        JSONObject jsonObject = options.getJSONObject(j);
                        String objectKey = (String) jsonObject.get(JsonFormConstants.KEY);
                        if (objectKey.equals(optionKey) && jsonObject.has(JsonFormConstants.SPECIFY_CONTENT)) {
                            if (extraFieldsWithValues != null) {
                                fields = extraFieldsWithValues;
                            } else {
                                fields = getSubFormFields(jsonObject.get(JsonFormConstants.SPECIFY_CONTENT).toString(), jsonObject.get(JsonFormConstants.SPECIFY_CONTENT_FORM).toString(), fields);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return fields;
    }

    private JSONArray getSubFormFields(String subFormName, String subFormLocation, JSONArray fields) {
        JSONArray fieldArray = new JSONArray();
        genericPopupDialog.setFormIdentity(subFormName);
        genericPopupDialog.setFormLocation(subFormLocation);
        JSONObject jsonObject = genericPopupDialog.getSubFormJson("", getApplicationContext());
        if (jsonObject != null) {
            try {
                JSONArray jsonArray = jsonObject.getJSONArray(JsonFormConstants.SPECIFY_CONTENT);
                if (jsonArray != null && jsonArray.length() > 0) {
                    fieldArray = formUtils.concatArray(fields, jsonArray);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return fieldArray;
    }


    public String getConfirmCloseTitle() {
        return confirmCloseTitle;
    }

    public void setConfirmCloseTitle(String confirmCloseTitle) {
        this.confirmCloseTitle = confirmCloseTitle;
    }

    public String getConfirmCloseMessage() {
        return confirmCloseMessage;
    }

    public void setConfirmCloseMessage(String confirmCloseMessage) {
        this.confirmCloseMessage = confirmCloseMessage;
    }

    private boolean isRelevant(Map<String, String> curValueMap, JSONObject curRelevance) throws Exception {
        if (curRelevance.has(JsonFormConstants.JSON_FORM_KEY.EX_CHECKBOX)) {
            JSONArray exArray = curRelevance.getJSONArray(JsonFormConstants.JSON_FORM_KEY.EX_CHECKBOX);

            for (int i = 0; i < exArray.length(); i++) {
                ExObjectResult exObjectResult = isExObjectRelevant(curValueMap, exArray.getJSONObject(i));
                if (exObjectResult.isRelevant()) {
                    return true;
                } else if (!exObjectResult.isRelevant() && exObjectResult.isFinal()) {
                    return false;
                }

            }
            return false;
        } else {
            return doComparison(curValueMap.get(JsonFormConstants.VALUE), curRelevance);
        }
    }

    private ExObjectResult isExObjectRelevant(Map<String, String> curValueMap, JSONObject object) throws Exception {
        if (object.has(JsonFormConstants.JSON_FORM_KEY.NOT)) {
            JSONArray orArray = object.getJSONArray(JsonFormConstants.JSON_FORM_KEY.NOT);

            for (int i = 0; i < orArray.length(); i++) {
                if (!Boolean.valueOf(curValueMap.get(orArray.getString(i)))) {
                    return new ExObjectResult(true, false);
                } else {
                    return new ExObjectResult(false, true);
                }
            }
        }

        if (object.has(JsonFormConstants.JSON_FORM_KEY.OR)) {
            JSONArray orArray = object.getJSONArray(JsonFormConstants.JSON_FORM_KEY.OR);

            for (int i = 0; i < orArray.length(); i++) {
                if (Boolean.valueOf(curValueMap.get(orArray.getString(i)))) {
                    return new ExObjectResult(true, true);
                }

            }

        }

        if (object.has(JsonFormConstants.JSON_FORM_KEY.AND)) {
            JSONArray andArray = object.getJSONArray(JsonFormConstants.JSON_FORM_KEY.AND);

            for (int i = 0; i < andArray.length(); i++) {
                if (!Boolean.valueOf(curValueMap.get(andArray.getString(i)))) {
                    return new ExObjectResult(false, false);
                }
            }
            return new ExObjectResult(true, false);

        }

        return new ExObjectResult(false, false);
    }

    /**
     * Receives the generic popup data from Generic Dialog fragment
     *
     * @param selectedValues
     * @param parentKey
     * @param stepName
     * @param childKey
     */
    @Override
    public void onGenericDataPass(Map<String, SecondaryValueModel> selectedValues, String parentKey, String stepName, String childKey) {
        if (mJSONObject != null) {
            JSONObject parentJson = getStep(stepName);
            JSONArray fields = new JSONArray();
            try {
                if (parentJson.has(JsonFormConstants.SECTIONS) && parentJson.get(JsonFormConstants.SECTIONS) instanceof JSONArray) {
                    JSONArray sections = parentJson.getJSONArray(JsonFormConstants.SECTIONS);
                    for (int i = 0; i < sections.length(); i++) {
                        JSONObject sectionJson = sections.getJSONObject(i);
                        if (sectionJson.has(JsonFormConstants.FIELDS)) {
                            fields = formUtils.concatArray(fields, sectionJson.getJSONArray(JsonFormConstants.FIELDS));
                        }
                    }
                } else if (parentJson.has(JsonFormConstants.FIELDS) && parentJson.get(JsonFormConstants.FIELDS) instanceof JSONArray) {
                    fields = parentJson.getJSONArray(JsonFormConstants.FIELDS);

                }

                if (fields.length() > 0) {
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject item = fields.getJSONObject(i);
                        if (item != null && item.getString(JsonFormConstants.KEY).equals(parentKey)) {
                            addSecondaryValues(getJsonObjectToUpdate(item, childKey), selectedValues);
                        }
                    }
                }

            } catch (JSONException e) {
                Log.i(TAG, Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Finds the actual widget to be updated and secondary values added on
     *
     * @param jsonObject
     * @param childKey
     * @return
     */
    private JSONObject getJsonObjectToUpdate(JSONObject jsonObject, String childKey) {
        JSONObject item = new JSONObject();
        try {
            if (jsonObject != null && jsonObject.has(JsonFormConstants.TYPE)) {
                if ((jsonObject.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.NATIVE_RADIO_BUTTON) || jsonObject.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.NATIVE_RADIO_BUTTON)) && childKey != null) {
                    JSONArray options = jsonObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                    if (options != null) {
                        for (int i = 0; i < options.length(); i++) {
                            JSONObject childItem = options.getJSONObject(i);
                            if (childItem != null && childItem.has(JsonFormConstants.KEY) && childKey.equals(childItem.getString(JsonFormConstants.KEY))) {
                                item = childItem;
                            }
                        }
                    }
                } else {
                    item = jsonObject;
                }
            } else {
                item = jsonObject;
            }
        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));
        }

        return item;
    }


    /**
     * Adding the secondary values on to the specific json widget
     *
     * @param item
     * @param secondaryValueModel
     */
    private void addSecondaryValues(JSONObject item, Map<String, SecondaryValueModel> secondaryValueModel) {
        JSONObject valueObject;
        JSONArray secondaryValuesArray = new JSONArray();
        SecondaryValueModel secondaryValue;
        for (Object o : secondaryValueModel.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            secondaryValue = (SecondaryValueModel) pair.getValue();
            valueObject = createSecondaryValueObject(secondaryValue);
            secondaryValuesArray.put(valueObject);
        }
        try {
            item.put(JsonFormConstants.SECONDARY_VALUE, secondaryValuesArray);
        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * Creates the secondary values objects
     *
     * @param value
     * @return
     */
    private JSONObject createSecondaryValueObject(SecondaryValueModel value) {
        JSONObject jsonObject = new JSONObject();
        try {
            String key = value.getKey();
            String type = value.getType();
            JSONArray values = value.getValues();

            jsonObject.put(JsonFormConstants.KEY, key);
            jsonObject.put(JsonFormConstants.TYPE, type);
            jsonObject.put(JsonFormConstants.VALUES, values);
        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));

        }
        return jsonObject;
    }
}
