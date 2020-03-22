package edu.uci.ics.charm.tabellion;

/*
Created Date: 07/30/2019
Created By: Myles Liu
Last Modified: 08/26/2019
Last Modified By: Myles Liu
Notes:
    1. Constructed followed by instruction of
    https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa

 */

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ContractBlock {

    public String hash;
    public String previousHash;
    private Contract contract;
    private long timeStamp;

    public ContractBlock(){

    }

    public ContractBlock(Contract contract, String previousHash){
        this.contract = contract;
        this.previousHash = previousHash;
        this.timeStamp = System.currentTimeMillis();
        this.hash = caculateHash();
    }

    public String getContractGsonString(){
        Gson gson = new Gson();
        return gson.toJson(contract);
    }

    public Contract getContract(){
        return contract;
    }

    public String getPrviousHash(){
        return previousHash;
    }

    private String caculateHash(){
        return applySha256(previousHash + timeStamp +
                getContractGsonString());
    }

    private static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Applies sha256 to our input,
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(); // This will contain hash as hexidecimal
            for (byte hash1 : hash) {
                String hex = Integer.toHexString(0xff & hash1);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
