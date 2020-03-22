package edu.uci.ics.charm.tabellion;

import com.google.gson.Gson;

import java.util.ArrayList;

/*
Created Date: 07/30/2019
Created By: Myles Liu
Last Modified: 09/16/2019
Last Modified By: Myles Liu
Notes:
    1. Constructed followed by instruction of
    https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa

 */

public class ContractBlockChain {

    private ArrayList<ContractBlock> contractBlocks = new ArrayList<>();

    public ContractBlockChain(){

    }

    public ContractBlockChain(Contract firstContract){
        contractBlocks.add(new ContractBlock(firstContract, "0"));
    }

    public void addNewBlock(Contract contract){
        contract.setRevisionCount(contractBlocks.size() - 1);
        contractBlocks.add(new ContractBlock(contract, contractBlocks.get(contractBlocks.size() - 1).hash));
    }

    public Contract getContract(int blockNum) throws IndexOutOfBoundsException {
        if(blockNum >= getTotalBlockNums()){
            throw new IndexOutOfBoundsException("There is no enough Block");
        }
        Gson gson = new Gson();
        return contractBlocks.get(blockNum).getContract();
    }

    public ContractBlock getContractBlock(int blockNum) throws IndexOutOfBoundsException {
        if(blockNum >= getTotalBlockNums()){
            throw new IndexOutOfBoundsException("There is no enough Block");
        }
        return contractBlocks.get(blockNum);
    }

    public void updateLastBlockWithContract(Contract contract){
        String lastPreviousHash = contractBlocks.get(getTotalBlockNums() - 1).getPrviousHash();
        contractBlocks.remove(getTotalBlockNums() - 1);
        contractBlocks.add(new ContractBlock(contract, lastPreviousHash));
    }

    public Contract getLatestContract(){
        return getContract(getTotalBlockNums() - 1);
    }

    public int getTotalBlockNums(){
        return contractBlocks.size();
    }
}
