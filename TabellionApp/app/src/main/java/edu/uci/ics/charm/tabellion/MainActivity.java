package edu.uci.ics.charm.tabellion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator;

/*
Created Date: 01/23/2019
Created By: Myles Liu
Last Modified: 03/22/2020
Last Modified By: Myles Liu
Notes:

Pending To Do:
    1. make initContracts() work properly but not just testing.
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private List<Contract> contractList = new ArrayList<>();
    private Context context = this;
    private RecyclerView recyclerView;
    public static Context sContext;
    private ContractAdapter contractAdapter;
    private MyApp myApp;
    private ProgressBar progressBar;
    private int countOfPreparingContracts = 0; // For syncing with servers when opening the app

    private int countOfOpenSecretMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        myApp = (MyApp) getApplication();

        sContext = getApplicationContext();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_pendingToSign);

        // Setting up user photo and name on nav header
        LinearLayout navHeader = (LinearLayout) navigationView.getHeaderView(0);

        CircleImageView imageView = (CircleImageView) navHeader.findViewById(R.id.nav_header_main_imageView);
        imageView.setImageBitmap(myApp.getCurrentUserPhoto());

        navHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(++countOfOpenSecretMode == 5){
                    if(myApp.isTransitionAndFreezingOn()){
                        myApp.setTransitionAndFreezingSwitch(false);
                        Toast.makeText(context, "The guardian is gone...", Toast.LENGTH_SHORT).show();
                    } else {
                        myApp.setTransitionAndFreezingSwitch(true);
                        Toast.makeText(context, "The guardian is back!", Toast.LENGTH_SHORT).show();
                    }
                    countOfOpenSecretMode = 0;
                }
            }
        });

        TextView textView = navHeader.findViewById(R.id.nav_header_main_textView);
        textView.setText(String.format(getString(R.string.hello), myApp.getCurrentUserFirstName()));
        // nav header set up end

        /*
        // Should have a handler for making and stopping uploading animation
        // Also, it should check whether the key has been uploaded before
        Connection connection = new Connection(context, myApp);
        connection.new UploadPublicKey().execute();
        */

        // Start of setting up SwipeRefreshLayout
        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.content_main_swipeRefresh_layout);
        swipeRefreshLayout.setNestedScrollingEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onResume();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // End of setting up SwipeRefreshLayout

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, NewDocument.class));
                // appendToTopContractList(new Contract("Example New Contract"));
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // initPendingToSignContracts();

        recyclerView = (RecyclerView) findViewById(R.id.content_main_recycler_view);
        recyclerView.setItemAnimator(new FadeInLeftAnimator());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        contractAdapter = new ContractAdapter(contractList, context, myApp);
        recyclerView.setAdapter(contractAdapter);
        if(contractList.size() > 0){
            contractAdapter.notifyItemRangeInserted(0, contractList.size() - 1);
        }

        progressBar = findViewById(R.id.content_main_progressBar);

    }

    @Override
    protected void onResume() {
        super.onResume();
        syncContractWithServer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause!");
    }

    private void prepareContracts(List<String> contractIDs){
        final int numOfContracts = contractIDs.size();
        if(numOfContracts == 0){
            refreshUI();
        } else {
            for(final String contractID: contractIDs){
                Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        ArrayList<String> contractInfo = msg.getData().getStringArrayList("contractInfo");
                        Log.d(TAG, "prepareContracts: " + "going to add or update temp_contract " + contractID + " with info: " + contractInfo);
                        myApp.addOrUpdateToCorrespondingContractList(myApp.newContractByInfo(contractInfo, contractID));
                        ++countOfPreparingContracts;
                        if(countOfPreparingContracts == numOfContracts){
                            countOfPreparingContracts = 0;
                            refreshUI();
                        } else {
                            progressBar.setProgress((countOfPreparingContracts / numOfContracts) * 100);
                        }
                        return false;
                    }
                });
                new Thread(new Connection(context, myApp).new GetOneContract(handler, contractID)).start();
            }
        }
    }

    public void syncContractWithServer(){
        if(myApp.isRefreshingOnMainActivity()){
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        myApp.setIsRefreshingOnMainActivity(true);
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                ArrayList<String> contractIDs = msg.getData().getStringArrayList("contractIDs");
                Log.d(TAG, "syncContractWithServer: what we get from server: " + contractIDs);
                Log.d(TAG, "syncContractWithServer: what we get from local: " + myApp.getAllContractIDs());
                List<String> localContractIDs = myApp.getAllContractIDs();

                if(contractIDs != null && !contractIDs.isEmpty()){
                    for(String localContractID: localContractIDs){
                        if(!contractIDs.contains(localContractID)){
                            myApp.removeContractFromCorrespondingContractList(localContractID);
                        }
                    }
                    prepareContracts(contractIDs);
                } else {
                    for(String localContractID: localContractIDs){
                        myApp.removeContractFromCorrespondingContractList(localContractID);
                    }
                    refreshUI();
                }
                return false;
            }
        });
        new Thread(new Connection(context, myApp).new SyncAllContracts(handler, myApp.getEmailAddress())).start();
    }

    private void checkIfAnyNewContract(){
        // Check if there is any new temp_contract for the user to sign
        // This is not working at all! (pending delete)

        String contractGsonData = getIntent().getStringExtra("contractFromOfferorFCM");
        if(contractGsonData != null){
            Gson gson = new Gson();
            Contract contract = (Contract) gson.fromJson(contractGsonData, new TypeToken<Contract>() {}.getType());
            Log.d(TAG, "onCreate: We got a new temp_contract from offeror with ID: " + contract.getContractId());
        } else {
            Log.d(TAG, "No new temp_contract from offeror is detected!");
        }
    }

    private void refreshUI(){
        if(myApp.getCurrentMode() == 0){
            refreshContractList(myApp.getSavedPendingToSignContractList());
        } else if(myApp.getCurrentMode() == 1){
            refreshContractList(myApp.getSavedInProgressContractList());
        } else {
            refreshContractList(myApp.getSavedHistoryContractList());
        }
        progressBar.setVisibility(View.GONE);
        myApp.setIsRefreshingOnMainActivity(false);
    }

    private void refreshContractList(List<Contract> newPendingToSignContractLists) {
        // This function is not working properly in some situations!!! (pending fix)
        List<Integer> pendingDeletedContracts = new ArrayList<>();
        List<Integer> pendingAddedContracts = new ArrayList<>();
        for(Contract contract: contractList){
            boolean going_to_delete = true;
            for(Contract contract_to_verify: newPendingToSignContractLists){
                if(contract.equals(contract_to_verify)){
                    going_to_delete = false;
                    break;
                }
            }
            if(going_to_delete){
                pendingDeletedContracts.add(contractList.indexOf(contract));
            }
        }
        Log.d("refreshPendingToSign", " going to delete: " + pendingDeletedContracts);
        deleteAndNotify(contractList, pendingDeletedContracts, contractAdapter);
        for(Contract contract: newPendingToSignContractLists){
            boolean going_to_add = true;
            for(Contract contract_to_verify: contractList){
                if(contract_to_verify.equals(contract)){
                    going_to_add = false;
                    break;
                }
            }
            if(going_to_add){
                pendingAddedContracts.add(newPendingToSignContractLists.indexOf(contract));
            }
        }
        addAndNotify(contractList, newPendingToSignContractLists, pendingAddedContracts, contractAdapter);
    }

    private void deleteAndNotify(List<Contract> contractList, List<Integer> pendingDeletedContracts,
                                 ContractAdapter contractAdapter) {
        // This function still needs to be tested
        Collections.reverse(pendingDeletedContracts);
        for(Integer indexToDelete: pendingDeletedContracts){
            Log.d("deleteAndNotify", " Going to delete " + indexToDelete + " from " + contractList);
            contractList.remove((int)indexToDelete);
            contractAdapter.notifyItemRemoved(indexToDelete);
        }
    }

    private void addAndNotify(List<Contract> contractList, List<Contract> pendingAddedContracts,
                              List<Integer> pendingAddedContractsIndex, ContractAdapter contractAdapter){
        // This function still needs to be tested
        for(Integer indexToAdd: pendingAddedContractsIndex){
            contractList.add(indexToAdd, pendingAddedContracts.get(indexToAdd));
            contractAdapter.notifyItemInserted(indexToAdd);
        }
    }

    private void initPendingToSignContracts() {
        contractList = myApp.getSavedPendingToSignContractList();
        // Right now this is just to testing
        /*
        for(int i = 0; i < 2; ++i){
            contractList.add(new Contract("Example Contract"));
        }
        */
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_pendingToSign) {
            myApp.setCurrentMode(0);
            syncContractWithServer();
        } else if (id == R.id.nav_waitForFinishing) {
            myApp.setCurrentMode(1);
            syncContractWithServer();
        } else if (id == R.id.nav_historyContracts) {
            myApp.setCurrentMode(2);
            syncContractWithServer();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_new_document) {
            startActivity(new Intent(this, NewDocument.class));
        } else if (id == R.id.nav_how_to) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void appendToTopContractList(Contract contract){
        // If not used, pending delete
        contractList.add(0, contract);
        contractAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    private void deleteFromContractList(int position){
        // If not used, pending delete
        contractList.remove(position);
        contractAdapter.notifyItemRemoved(position);
    }
}
