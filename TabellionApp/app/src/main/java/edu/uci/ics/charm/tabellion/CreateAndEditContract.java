package edu.uci.ics.charm.tabellion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

/*
Created Date: 09/28/2019
Created By: Myles Liu
Last Modified: 11/11/2019
Last Modified By: Myles Liu
Notes:
 */

public class CreateAndEditContract extends AppCompatActivity {

    private final static String TAG = "CreateAndEditContract";
    private MyApp myApp;

    private static final int CREATE_CONTRACT_CODE = 1;

    private LinearLayout editTextLayout;
    private Button nextButton;
    private Button previousButton;
    private Button saveAndBackButton;

    private ArrayList<EditText> contractEditTexts = new ArrayList<>();
    private ArrayList<String> contractEditTextStrings = new ArrayList<>();  // For reading strings from file only

    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_contract);

        myApp = (MyApp) getApplication();

        editTextLayout = (LinearLayout) findViewById(R.id.activity_create_contract_editText_layout);
        nextButton = (Button) findViewById(R.id.activity_create_contract_new_page_button);
        previousButton = (Button) findViewById(R.id.activity_create_contract_previous_page_button);
        saveAndBackButton = (Button) findViewById(R.id.activity_create_contract_save_and_back_button);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        String contractName = intent.getStringExtra("contract_name");
        final String possibleContractFilePath = intent.getStringExtra("contract_file_path");

        String contractTitle = intent.getStringExtra("contract_title");
        String contractDetails = intent.getStringExtra("contract_details");

        if (contractTitle != null && contractDetails != null){
            contractName = contractTitle;
            editTextLayout.addView(getNewSectionOfCreatingContract());
            contractEditTexts.get(0).setText(contractName);
            contractEditTexts.get(2).setText(contractDetails);
        } else if (possibleContractFilePath == null || possibleContractFilePath.isEmpty()){
            // Add one empty section to the view
            editTextLayout.addView(getNewSectionOfCreatingContract());
            contractEditTexts.get(0).setText(contractName);
        } else {
            String contractPureString = "";
            try {
                contractPureString = myApp.getStringFromFile(possibleContractFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String[] contractPureStrings = contractPureString.split("\n");
            int numOfLinesForPureStrings = contractPureStrings.length;
            int currentReadingMode = 0;
            int currentReadingModeCount = 0;    // How many lines have been read in the current mode
            StringBuilder currentReadingModeStringBuilder = new StringBuilder();

            for(int i = 4; i < numOfLinesForPureStrings; ++i){
                Log.d(TAG, "onCreate: Line " + i + ": " + contractPureStrings[i]);
                if(contractPureStrings[i].contains("[comment]: <> (main)")){

                    if(currentReadingMode != 0){
                        if(currentReadingModeCount == 0){
                            contractEditTextStrings.add("");
                        } else {
                            currentReadingModeStringBuilder.deleteCharAt(currentReadingModeStringBuilder.length() - 1);
                            contractEditTextStrings.add(currentReadingModeStringBuilder.toString());
                        }
                        currentReadingModeStringBuilder = new StringBuilder();
                        currentReadingModeCount = 0;
                    }

                    currentReadingMode = 1;

                } else if (contractPureStrings[i].contains("[comment]: <> (sub)")){

                    if(currentReadingMode != 0){
                        if(currentReadingModeCount == 0){
                            contractEditTextStrings.add("");
                        } else {
                            currentReadingModeStringBuilder.deleteCharAt(currentReadingModeStringBuilder.length() - 1);
                            contractEditTextStrings.add(currentReadingModeStringBuilder.toString());
                        }
                        currentReadingModeStringBuilder = new StringBuilder();
                        currentReadingModeCount = 0;
                    }

                    currentReadingMode = 2;

                } else if (contractPureStrings[i].contains("[comment]: <> (details)")){

                    if(currentReadingMode != 0){
                        if(currentReadingModeCount == 0){
                            contractEditTextStrings.add("");
                        } else {
                            currentReadingModeStringBuilder.deleteCharAt(currentReadingModeStringBuilder.length() - 1);
                            contractEditTextStrings.add(currentReadingModeStringBuilder.toString());
                        }
                        currentReadingModeStringBuilder = new StringBuilder();
                        currentReadingModeCount = 0;
                    }

                    currentReadingMode = 3;

                } else if (contractPureStrings[i].contains("[comment]: <> (")){

                    if(currentReadingMode != 0){
                        if(currentReadingModeCount == 0){
                            contractEditTextStrings.add("");
                        } else {
                            currentReadingModeStringBuilder.deleteCharAt(currentReadingModeStringBuilder.length() - 1);
                            contractEditTextStrings.add(currentReadingModeStringBuilder.toString());
                        }
                        currentReadingModeStringBuilder = new StringBuilder();
                        currentReadingModeCount = 0;
                    }

                    currentReadingMode = 0;

                } else {
                    switch (currentReadingMode){
                        case 1:
                            if(contractPureStrings[i].contains("#")){
                                currentReadingModeStringBuilder.append(contractPureStrings[i].substring(1));
                                currentReadingModeStringBuilder.append("\n");
                                ++currentReadingModeCount;
                            }
                            break;
                        case 2:
                            if(contractPureStrings[i].contains("**")){
                                currentReadingModeStringBuilder.append(contractPureStrings[i].substring(2, contractPureStrings[i].length() - 2));
                                currentReadingModeStringBuilder.append("\n");
                                ++currentReadingModeCount;
                            }
                            break;
                        case 3:
                            currentReadingModeStringBuilder.append(contractPureStrings[i]);
                            currentReadingModeStringBuilder.append("\n");
                            ++currentReadingModeCount;
                            break;
                    }
                }
            }

            if(currentReadingMode != 0){
                if(currentReadingModeCount == 0){
                    contractEditTextStrings.add("");
                } else {
                    currentReadingModeStringBuilder.deleteCharAt(currentReadingModeStringBuilder.length() - 1);
                    contractEditTextStrings.add(currentReadingModeStringBuilder.toString());
                }
            }

            Log.d(TAG, "onCreate: final Strings we have: " + contractEditTextStrings);

            int numOfSections = contractEditTextStrings.size() / 3;

            for(int i = 0; i < numOfSections; ++i){
                editTextLayout.addView(getNewSectionOfCreatingContract());
            }

            int numOfSubSections = contractEditTextStrings.size();

            for(int i = 0; i < numOfSubSections; ++i){
                contractEditTexts.get(i).setText(contractEditTextStrings.get(i));
            }

        }

        previousButton.setVisibility(View.GONE);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextLayout.addView(getNewSectionOfCreatingContract());
            }
        });

        saveAndBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder finalContractStringBuilder = new StringBuilder();
                finalContractStringBuilder
                        .append("[comment]: <> (")
                        .append(System.currentTimeMillis())
                        .append(")\n\n\n\n");

                int accountOfEditTexts = contractEditTexts.size();
                for(int i = 0; i < accountOfEditTexts; ++i){
                    int mode = i % 3;   // 0 means first, 1 means second, 2 means third
                    int sectionCount = i / 3;
                    switch (mode){
                        case 0:
                            finalContractStringBuilder
                                    .append("[comment]: <> (")
                                    .append(sectionCount)
                                    .append(")\n");
                            finalContractStringBuilder
                                    .append("[comment]: <> (main)\n");
                            if(!contractEditTexts.get(i).getText().toString().isEmpty()){
                                finalContractStringBuilder
                                        .append("#")
                                        .append(contractEditTexts.get(i).getText().toString())
                                        .append("\n\n");
                            }
                            break;
                        case 1:
                            finalContractStringBuilder
                                    .append("[comment]: <> (sub)\n");
                            if(!contractEditTexts.get(i).getText().toString().isEmpty()){
                                finalContractStringBuilder
                                        .append("**")
                                        .append(contractEditTexts.get(i).getText().toString())
                                        .append("**\n\n");
                            }
                            break;
                        case 2:
                            finalContractStringBuilder
                                    .append("[comment]: <> (details)\n");
                            if(!contractEditTexts.get(i).getText().toString().isEmpty()){
                                finalContractStringBuilder
                                        .append(contractEditTexts.get(i).getText().toString())
                                        .append("\n\n");
                            }
                            break;
                    }
                }

                String contractFileName = "temp_contract.md";

                String contractFilePath = myApp.writeStringToFile(finalContractStringBuilder.toString(),
                        "temp_contract.md", "temp_for_creating_contract");

                if(possibleContractFilePath != null && !possibleContractFilePath.isEmpty()){
                    myApp.writeStringToExistingFile(finalContractStringBuilder.toString(), possibleContractFilePath);
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("contract_file_name", contractFileName);
                resultIntent.putExtra("contract_file_path", contractFilePath);

                setResult(RESULT_OK, resultIntent);

                finish();

            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private View getNewSectionOfCreatingContract(){

        // Create Root Layout
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        // Create First EditText
        EditText editText1 = new EditText(this);
        LinearLayout.LayoutParams editText1LayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText1LayoutParams.setMargins(0, 0, 0, 50);
        editText1.setLayoutParams(editText1LayoutParams);
        editText1.setHint(R.string.type_contract_section_main_title);

        // Create Second EditText
        EditText editText2 = new EditText(this);
        LinearLayout.LayoutParams editText2LayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText2LayoutParams.setMargins(0, 0, 0, 50);
        editText2.setLayoutParams(editText2LayoutParams);
        editText2.setHint(R.string.type_contract_section_sub_title);

        // Create Third EditText
        EditText editText3 = new EditText(this);
        LinearLayout.LayoutParams editText3LayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText3LayoutParams.setMargins(0, 0, 0, 50);
        editText3.setLayoutParams(editText3LayoutParams);
        editText3.setMinLines(10);
        editText3.setHint(R.string.type_contract_section_details);

        // Add all views to Root Layout
        linearLayout.addView(editText1);
        linearLayout.addView(editText2);
        linearLayout.addView(editText3);

        // Add all EditText to the EditText ArrayList
        contractEditTexts.add(editText1);
        contractEditTexts.add(editText2);
        contractEditTexts.add(editText3);

        return linearLayout;
    }

}
