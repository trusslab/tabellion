package edu.uci.ics.charm.tabellion;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/*
Created Date: 01/24/2019
Created By: Myles Liu
Last Modified: 04/22/2019
Last Modified By: Myles Liu
Notes:

 */

public class ContractAdapter extends RecyclerView.Adapter<ContractAdapter.ViewHolder> {

    private final static String TAG = "ContractAdapter";
    private List<Contract> mContractList;
    private Context context;
    private MyApp myApp;
    private Handler handler;
    private TextView textView;
    private CircleImageView imageView;
    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    private Handler handlerForUpdatingContractConfirmingStatus;
    private ProgressDialog progressDialog;


    static class ViewHolder extends RecyclerView.ViewHolder {
        View contractView;
        ImageView contractPreviewImageView;
        TextView contractNameTextView;
        TextView contractDescriptionTextView;
        TextView contractCurrentStatusTextView;
        TextView contractOfferorEmailAddressTextView;
        TextView contractOffereeEmailAddressTextView;

        public ViewHolder(View view){
            super(view);
            contractView = view;
            contractPreviewImageView = (ImageView) view.findViewById(R.id.pending_sign_contract_item_preview_image_view);
            contractNameTextView = (TextView) view.findViewById(R.id.pending_sign_contract_item_contract_name_text_view);
            contractDescriptionTextView = (TextView) view.findViewById(R.id.pending_sign_contract_item_contract_description_text_view);
            contractCurrentStatusTextView = (TextView) view.findViewById(R.id.pending_sign_contract_item_current_role_text_view);
            contractOfferorEmailAddressTextView = (TextView) view.findViewById(R.id.pending_sign_contract_item_offeror_email_text_view);
            contractOffereeEmailAddressTextView = (TextView) view.findViewById(R.id.pending_sign_contract_item_offeree_email_text_view);
        }
    }

    public ContractAdapter(List<Contract> contractList, Context context, MyApp myApp){
        mContractList = contractList;
        this.context = context;
        this.myApp = myApp;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contract_item,
                viewGroup, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.contractView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myApp.isRefreshingOnMainActivity()){
                    Toast.makeText(context, R.string.wait_for_contracts_to_be_updated, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, " Trying to get " + holder.getAdapterPosition() + " from " + mContractList);
                myApp.setCurrentViewingContract(mContractList.get(holder.getAdapterPosition()));
                Log.d(TAG, " The confirmstatus is: " + myApp.getCurrentViewingContract().getConfirmStatus());
                if(myApp.getCurrentViewingContract().getCurrentRole() == 1 &&
                        myApp.getCurrentViewingContract().getContractStatus() == 1
                        && myApp.getCurrentViewingContract().getConfirmStatus() == 0){
                    startConfirmationPage();
                } else if (myApp.getCurrentViewingContract().getCurrentRole() == 1 && myApp.getCurrentViewingContract().getConfirmStatus() != 0){
                    Log.d(TAG, "Going To open contractViewingActivity!");
                    context.startActivity(new Intent(context, ContractViewingActivity.class));
                } else if (myApp.getCurrentViewingContract().getCurrentRole() == 0){
                    Log.d(TAG, "Going To open contractViewingActivity!");
                    context.startActivity(new Intent(context, ContractViewingActivity.class));
                } else {
                    Toast.makeText(context, R.string.wait_for_offeror_first_before_confirm, Toast.LENGTH_LONG).show();
                }
            }
        });

        View dialog_debug_view = LayoutInflater.from(context).inflate(R.layout.dialog_for_debugging_contract, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(dialog_debug_view);
        final EditText editText = (EditText) dialog_debug_view.findViewById(R.id.dialog_for_debugging_contract_editText);
        final CheckBox checkBox = (CheckBox) dialog_debug_view.findViewById(R.id.dialog_for_debugging_contract_checkbox);
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Contract currentContract = mContractList.get(holder.getAdapterPosition());
                String inputData = editText.getText().toString();
                if(!inputData.isEmpty()){
                    currentContract.setTransitionTime(Integer.valueOf(inputData));
                }
                currentContract.setIsSwipeBackEnabled(checkBox.isChecked());
                Log.d(TAG, "onCreateViewHolder: After checking isSwipeBackEnabled: " + currentContract.isSwipeBackEnabled());
                myApp.addOrUpdateToCorrespondingContractList(currentContract);
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialogBuilder.setCancelable(false);
        final AlertDialog alertDialog = alertDialogBuilder.create();
        holder.contractView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final Contract currentContract = mContractList.get(holder.getAdapterPosition());
                editText.setText(String.valueOf(currentContract.getTransitionTime()));
                if(myApp.isRefreshingOnMainActivity()){
                    Toast.makeText(context, R.string.wait_for_contracts_to_be_updated, Toast.LENGTH_SHORT).show();
                    return false;
                }
                checkBox.setChecked(mContractList.get(holder.getAdapterPosition()).isSwipeBackEnabled());
                alertDialog.show();
                return false;
            }
        });
        return holder;
    }

    private void startConfirmationPage(){
        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getString(R.string.notification));
        LayoutInflater factory = LayoutInflater.from(context);
        final View view = factory.inflate(R.layout.dialog_for_confirming_contract, null);
        imageView = view.findViewById(R.id.dialog_for_confirming_contract_user_photo);
        progressBar = view.findViewById(R.id.dialog_for_confirming_contract_progress_bar);
        textView = view.findViewById(R.id.dialog_for_confirming_text_content);
        textView.setText(R.string.loading);
        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        setUpHandlerForUpdatingContractConfirmingStatus();
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(context.getString(R.string.talking_with_server));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.confirm),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        progressDialog.show();
                        new Thread(new Connection(context, myApp).new UpdateContractConfirmStatus(
                                handlerForUpdatingContractConfirmingStatus,
                                myApp.getCurrentViewingContract().getContractId(),
                                1)).start();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
        setUpHandlerForUserPhotoViewing();
        new Thread(new Connection(context, myApp).new DownloadFile(
                myApp.getUserPhotoFileNameByEmail(myApp.getCurrentViewingContract().getOfferorEmailAddress()),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                        myApp.getCurrentViewingContract().getContractId(),
                "users/user_photos/", handler)).start();
    }

    private void setUpHandlerForUpdatingContractConfirmingStatus(){
        handlerForUpdatingContractConfirmingStatus = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(!msg.getData().getBoolean("is_success")){
                    Toast.makeText(context, context.getString(R.string.check_connection), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
                return false;
            }
        });
    }

    private void setUpHandlerForUserPhotoViewing(){
        // This is used when offeree need to confirm to receive temp_contract from a offeror (Should not be directly called)
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.getData().getBoolean("is_success")){
                    textView.setText(R.string.confirm_getting_contract_from_offeror);
                    imageView.setImageBitmap(myApp.getContractIDOppositeUserPhotoLocal(
                            myApp.getCurrentViewingContract().getContractId(),
                            myApp.getCurrentViewingContract().getOfferorEmailAddress()));
                    progressBar.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(context, context.getString(R.string.check_connection), Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Contract contract = mContractList.get(i);
        if(contract.getCurrentRole() == 1 && contract.getConfirmStatus() == 0){
            viewHolder.contractNameTextView.setText(context.getString(R.string.hidden));
            viewHolder.contractDescriptionTextView.setText(context.getString(R.string.hidden));
        } else {
            viewHolder.contractNameTextView.setText(contract.getContractName());
            viewHolder.contractDescriptionTextView.setText(contract.getContractDescription());
        }
        if(contract.getCurrentRole() == 0){
            viewHolder.contractPreviewImageView.setImageBitmap(BitmapFactory.
                    decodeResource(context.getResources(), R.drawable.sender));
        } else {
            viewHolder.contractPreviewImageView.setImageBitmap(BitmapFactory.
                    decodeResource(context.getResources(), R.drawable.receiver));
        }
        Log.d(TAG, "Status of Contract: " + contract.getContractStatus() + " and the role of current user is: " + contract.getCurrentRole());
        switch (contract.getContractStatus()){
            case 0:
                if(contract.getCurrentRole() == 0){
                    viewHolder.contractCurrentStatusTextView.setText(String.format(
                            (String)context.getResources().getText(R.string.current_status),
                            context.getResources().getText(R.string.offeror)));
                } else {
                    viewHolder.contractCurrentStatusTextView.setText(String.format(
                            (String)context.getResources().getText(R.string.current_status),
                            context.getResources().getText(R.string.wait_for_other_to_sign)));
                }
                break;
            case 1:
                if(contract.getCurrentRole() == 0){
                    viewHolder.contractCurrentStatusTextView.setText(String.format(
                            (String)context.getResources().getText(R.string.current_status),
                            context.getResources().getText(R.string.wait_for_other_to_sign)));
                } else {
                    viewHolder.contractCurrentStatusTextView.setText(String.format(
                            (String)context.getResources().getText(R.string.current_status),
                            context.getResources().getText(R.string.offeree)));
                }
                break;
            case 2:
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.finishing)));
                break;
            case 3:
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.revoked_by_offeror)));
                viewHolder.contractPreviewImageView.setImageResource(R.drawable.failed_contract);
                break;
            case 8:
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.awaiting_revoke)));
                break;
            case 7:
                viewHolder.contractPreviewImageView.setImageBitmap(BitmapFactory.
                        decodeResource(context.getResources(), R.drawable.handshake));
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.properly_finished)));
                break;
            case 10:
                // For offeror
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.verifying_signature)));
                break;
            case 11:
                // For offeree
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.verifying_signature)));
                break;
            default:
                viewHolder.contractCurrentStatusTextView.setText(String.format(
                        (String)context.getResources().getText(R.string.current_status),
                        context.getResources().getText(R.string.unknown_status)));
        }
        String offerorEmailAddress = context.getResources().getText(R.string.from) + contract.getOfferorEmailAddress();
        viewHolder.contractOfferorEmailAddressTextView.setText(offerorEmailAddress);
        String offereeEmailAddress = context.getResources().getText(R.string.to) + contract.getOffereeEmailAddress();
        viewHolder.contractOffereeEmailAddressTextView.setText(offereeEmailAddress);
    }

    @Override
    public int getItemCount() {
        return mContractList.size();
    }
}
